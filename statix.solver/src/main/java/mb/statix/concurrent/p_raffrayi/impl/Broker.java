package mb.statix.concurrent.p_raffrayi.impl;

import java.util.Map;
import java.util.Set;

import javax.annotation.Nullable;

import org.metaborg.util.task.ICancel;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;

import mb.statix.concurrent.actors.IActor;
import mb.statix.concurrent.actors.IActorRef;
import mb.statix.concurrent.actors.TypeTag;
import mb.statix.concurrent.actors.deadlock.Clock;
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

public class Broker<S, L, D, R> implements IBroker<S, L, D, R> {

    private final IScopeImpl<S> scopeImpl;
    private final Set<L> edgeLabels;
    private final ICancel cancel;

    private final ActorSystem system;
    private final IActor<DeadlockMonitor<IWaitFor>> dlm;

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
                self -> new DeadlockMonitor<>(self, (_1, _2) -> {
                }));

        this.units = Maps.newHashMap();
        this.results = Maps.newHashMap();
        this.result = new CompletableFuture<>();
    }

    @Override public void add(String id, ITypeChecker<S, L, D, R> unitChecker) {
        add(id, unitChecker, null);
    }

    private IActorRef<? extends IUnit2UnitProtocol<S, L, D>> add(String id, ITypeChecker<S, L, D, R> unitChecker,
            @Nullable S root) {
        final IActor<IUnit<S, L, D, R>> unit = system.add(id, TypeTag.of(IUnit.class),
                self -> new Unit<>(self, null, new UnitContext(self), unitChecker, edgeLabels));
        synchronized(lock) {
            units.put(id, unit);
        }
        system.async(unit)._start(root).whenComplete((r, ex) -> addResult(id, r, ex));
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

    @Override public void run() {
        system.start();

        // start cancel watcher
        final Thread watcher = new Thread(() -> {
            try {
                while(true) {
                    if(result.isDone()) {
                        return;
                    } else if(cancel.cancelled()) {
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

        private final IActor<? extends IUnit2UnitProtocol<S, L, D>> self;

        public UnitContext(IActor<? extends IUnit2UnitProtocol<S, L, D>> self) {
            this.self = self;
        }

        @Override public ICancel cancel() {
            return cancel;
        }

        @Override public S makeScope(String name) {
            return scopeImpl.make(self.id(), name);
        }

        @Override public IActorRef<? extends IUnit2UnitProtocol<S, L, D>> owner(S scope) {
            synchronized(lock) {
                return units.get(scopeImpl.id(scope));
            }
        }

        @Override public IActorRef<? extends IUnit2UnitProtocol<S, L, D>> add(String id,
                ITypeChecker<S, L, D, R> unitChecker, S root) {
            return Broker.this.add(id, unitChecker, root);
        }

        @Override public void waitFor(IWaitFor token, IActorRef<? extends IUnit2UnitProtocol<S, L, D>> unit) {
            self.async(dlm).waitFor(unit, token);
        }

        @Override public void granted(IWaitFor token, IActorRef<? extends IUnit2UnitProtocol<S, L, D>> unit) {
            self.async(dlm).granted(unit, token);
        }

        @Override public void suspended(Clock clock) {
            self.async(dlm).suspended(clock);
        }

        @Override public void stopped(Clock clock) {
            self.async(dlm).stopped(clock);
        }

    }

}