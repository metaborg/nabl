package mb.statix.p_raffrayi.impl;

import java.util.Map;
import java.util.Set;

import javax.annotation.Nullable;

import org.metaborg.util.task.ICancel;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;

import mb.statix.actors.IActor;
import mb.statix.actors.IActorRef;
import mb.statix.actors.TypeTag;
import mb.statix.actors.deadlock.DeadlockMonitor;
import mb.statix.actors.deadlock.IDeadlockMonitor;
import mb.statix.actors.futures.CompletableFuture;
import mb.statix.actors.futures.IFuture;
import mb.statix.actors.impl.ActorSystem;
import mb.statix.p_raffrayi.IBroker;
import mb.statix.p_raffrayi.IResult;
import mb.statix.p_raffrayi.IScopeImpl;
import mb.statix.p_raffrayi.ITypeChecker;

public class Broker<S, L, D, R> implements IBroker<S, L, D, R> {

    private final IScopeImpl<S> scopeImpl;
    private final Set<L> edgeLabels;
    private final ActorSystem system;
    private final IActor<DeadlockMonitor<IWaitFor>> dlm;
    private final Map<String, IActor<IUnit<S, L, D, R>>> units;

    public Broker(IScopeImpl<S> scopeImpl, Iterable<L> edgeLabels) {
        this.scopeImpl = scopeImpl;
        this.edgeLabels = ImmutableSet.copyOf(edgeLabels);
        this.system = new ActorSystem();
        this.dlm = system.add("<DLM>", TypeTag.of(IDeadlockMonitor.class),
                self -> new DeadlockMonitor<>(self, (_1, _2) -> {
                }));
        this.units = Maps.newHashMap();
    }

    @Override public void add(String id, ITypeChecker<S, L, D, R> unitChecker) {
        add(id, unitChecker, null);
    }

    private IActorRef<? extends IUnit2UnitProtocol<S, L, D>> add(String id, ITypeChecker<S, L, D, R> unitChecker,
            @Nullable S root) {
        final IActor<IUnit<S, L, D, R>> unit = system.add(id, TypeTag.of(IUnit.class),
                self -> new Unit<>(self, null, new UnitContext(self), unitChecker, edgeLabels));
        unit.addMonitor(system.async(dlm));
        units.put(id, unit);
        system.async(unit)._start(root);
        return unit;
    }

    @Override public IFuture<IResult<S, L, D, R>> run(ICancel cancel) {
        system.start();

        //        // start cancel watcher
        //        final Thread watcher = new Thread(() -> {
        //            try {
        //                while(true) {
        //                    Thread.sleep(1000);
        //                    if(cancel.cancelled()) {
        //                        executor.shutdownNow();
        //                        return;
        //                    }
        //                }
        //            } catch(InterruptedException e) {
        //            }
        //        }, "StatixWatcher");
        //        watcher.start();

        return new CompletableFuture<>();
    }

    private class UnitContext implements IUnitContext<S, L, D, R> {

        private final IActorRef<? extends IUnit2UnitProtocol<S, L, D>> self;

        public UnitContext(IActorRef<? extends IUnit2UnitProtocol<S, L, D>> self) {
            this.self = self;
        }

        @Override public ICancel cancel() {
            // TODO Implement method IUnitContext<S,L,D>::cancel.
            throw new UnsupportedOperationException("Method IUnitContext<S,L,D>::cancel not implemented.");
        }

        @Override public S makeScope(String name) {
            return scopeImpl.make(self.id(), name);
        }

        @Override public IActorRef<? extends IUnit2UnitProtocol<S, L, D>> owner(S scope) {
            return units.get(scopeImpl.id(scope));
        }

        @Override public IActorRef<? extends IUnit2UnitProtocol<S, L, D>> add(String id,
                ITypeChecker<S, L, D, R> unitChecker, S root) {
            return Broker.this.add(id, unitChecker, root);
        }

        @Override public void suspend(Clock<S, L, D> clock) {
            // TODO Implement
        }

        @Override public void waitFor(IWaitFor token, IActorRef<? extends IUnit2UnitProtocol<S, L, D>> unit) {
            // TODO Implement
        }

        @Override public void granted(IWaitFor token, IActorRef<? extends IUnit2UnitProtocol<S, L, D>> unit) {
            // TODO Implement
        }

    }

}