package mb.statix.concurrent.p_raffrayi.impl;

import java.util.Map;
import java.util.Set;

import javax.annotation.Nullable;

import org.metaborg.util.task.ICancel;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;

import mb.statix.concurrent.actors.IActor;
import mb.statix.concurrent.actors.IActorRef;
import mb.statix.concurrent.actors.TypeTag;
import mb.statix.concurrent.actors.deadlock.Clock;
import mb.statix.concurrent.actors.deadlock.Deadlock;
import mb.statix.concurrent.actors.deadlock.DeadlockMonitor;
import mb.statix.concurrent.actors.deadlock.IDeadlockMonitor;
import mb.statix.concurrent.actors.futures.CompletableFuture;
import mb.statix.concurrent.actors.futures.IFuture;
import mb.statix.concurrent.actors.impl.ActorSystem;
import mb.statix.concurrent.p_raffrayi.IBroker;
import mb.statix.concurrent.p_raffrayi.IResult;
import mb.statix.concurrent.p_raffrayi.IScopeImpl;
import mb.statix.concurrent.p_raffrayi.ITypeChecker;
import mb.statix.concurrent.p_raffrayi.IUnitResult;
import mb.statix.concurrent.p_raffrayi.impl.tokens.IWaitFor;

public class Broker<S, L, D, R> implements IBroker<S, L, D, R> {

    private final IScopeImpl<S> scopeImpl;
    private final Set<L> edgeLabels;
    private final ICancel cancel;

    private final ActorSystem system;
    private final IActor<IDeadlockMonitor<IUnit<S, L, D, R>, UnitState, IWaitFor<S, L, D>>> dlm;

    private final Object lock = new Object();
    private final Map<String, IActor<IUnit<S, L, D, R>>> units;
    private final Map<String, IUnitResult<S, L, D, R>> results;
    private final CompletableFuture<IResult<S, L, D, R>> result;

    public Broker(IScopeImpl<S> scopeImpl, Iterable<L> edgeLabels, ICancel cancel) {
        this.scopeImpl = scopeImpl;
        this.edgeLabels = ImmutableSet.copyOf(edgeLabels);
        this.cancel = cancel;

        this.system = new ActorSystem();
        this.dlm = system.add("<DLM>", TypeTag.of(IDeadlockMonitor.class),
                self -> new DeadlockMonitor<>(self, this::handleDeadlock));

        this.units = Maps.newHashMap();
        this.results = Maps.newHashMap();
        this.result = new CompletableFuture<>();
    }

    @Override public void add(String id, ITypeChecker<S, L, D, R> unitChecker) {
        final IActor<IUnit<S, L, D, R>> unit = add(id, null, unitChecker);
        system.async(unit)._start(null);
    }

    private IActor<IUnit<S, L, D, R>> add(String id, @Nullable IActorRef<? extends IUnit<S, L, D, R>> parent,
            ITypeChecker<S, L, D, R> unitChecker) {
        final IActor<IUnit<S, L, D, R>> unit = system.add(id, TypeTag.of(IUnit.class),
                self -> new Unit<>(self, parent, new UnitContext(self), unitChecker, edgeLabels));
        synchronized(lock) {
            units.put(id, unit);
        }
        return unit;
    }

    private void addResult(String id, IUnitResult<S, L, D, R> unitResult, Throwable ex) {
        synchronized(lock) {
            if(ex != null) {
                result.completeExceptionally(ex);
                // TODO Existing units may still complete results
                system.stop();
            } else {
                results.put(id, unitResult);
                if(results.size() == units.size()) {
                    result.completeValue(Result.of(results));
                    system.stop();
                }
            }
        }
    }

    private void handleDeadlock(IActor<?> dlm,
            Deadlock<IActorRef<? extends IUnit<S, L, D, R>>, UnitState, IWaitFor<S, L, D>> deadlock) {
        if(deadlock.edges().isEmpty()) {
            final IActorRef<? extends IUnit<S, L, D, R>> unit = Iterables.getOnlyElement(deadlock.nodes().keySet());
            dlm.async(unit)._done().whenComplete((r, ex) -> addResult(unit.id(), r, ex));
        } else {
            for(IActorRef<? extends IUnit<S, L, D, R>> unit : deadlock.nodes().keySet()) {
                dlm.async(unit)._fail();
            }
        }
    }

    @Override public void run() {
        system.start();

        // start cancel watcher
        final Thread watcher = new Thread(() -> {
            try {
                while(true) {
                    if(result.isDone()) {
                        return;
                    } else if(cancel.cancelled()) {
                        result.completeExceptionally(new InterruptedException());
                        system.stop();
                        return;
                    } else {
                        Thread.sleep(1000);
                    }
                }
            } catch(InterruptedException e) {
            }
        }, "PRaffrayiWatcher");
        watcher.start();
    }

    @Override public IFuture<IResult<S, L, D, R>> result() {
        return result;
    }

    private class UnitContext implements IUnitContext<S, L, D, R> {

        private final IActor<? extends IUnit<S, L, D, R>> self;

        public UnitContext(IActor<? extends IUnit<S, L, D, R>> self) {
            this.self = self;
        }

        @Override public ICancel cancel() {
            return cancel;
        }

        @Override public S makeScope(String name) {
            return scopeImpl.make(self.id(), name);
        }

        @Override public IActorRef<? extends IUnit<S, L, D, R>> owner(S scope) {
            synchronized(lock) {
                return units.get(scopeImpl.id(scope));
            }
        }

        @Override public IActorRef<? extends IUnit<S, L, D, R>> add(String id, ITypeChecker<S, L, D, R> unitChecker,
                S root) {
            final IActor<IUnit<S, L, D, R>> unit = Broker.this.add(id, self, unitChecker);
            self.async(unit)._start(root);
            return unit;
        }

        @Override public void waitFor(IWaitFor<S, L, D> token, IActorRef<? extends IUnit<S, L, D, R>> unit) {
            self.async(dlm).waitFor(unit, token);
        }

        @Override public void granted(IWaitFor<S, L, D> token, IActorRef<? extends IUnit<S, L, D, R>> unit) {
            self.async(dlm).granted(unit, token);
        }

        @Override public void suspended(UnitState state, Clock<IUnit<S, L, D, R>> clock) {
            self.async(dlm).suspended(state, clock);
        }

        @Override public void stopped(Clock<IUnit<S, L, D, R>> clock) {
            self.async(dlm).stopped(clock);
        }

    }

}