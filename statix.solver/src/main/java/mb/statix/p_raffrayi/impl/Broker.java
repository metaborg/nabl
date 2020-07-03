package mb.statix.p_raffrayi.impl;

import java.util.Map;
import java.util.Set;

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

public class Broker<S, L, D> implements IBroker<S, L, D> {

    public static final String PROJECT_ID = "<>";

    private final IScopeImpl<S> scopeImpl;
    private final S root;
    private final Set<L> edgeLabels;
    private final ActorSystem system;
    private final IActor<DeadlockMonitor<IProtocolToken>> dlm;
    private final IActor<IProject<S, L, D>> project;
    private final Map<String, IActor<IUnit<S, L, D>>> units;

    public Broker(IScopeImpl<S> scopeImpl, Iterable<L> edgeLabels) {
        this.scopeImpl = scopeImpl;
        this.root = scopeImpl.make(PROJECT_ID, "0");
        this.edgeLabels = ImmutableSet.copyOf(edgeLabels);
        this.system = new ActorSystem();
        this.dlm = system.add("<DLM>", TypeTag.of(IDeadlockMonitor.class), self -> new DeadlockMonitor<>(self, (_1, _2) -> {}));
        this.units = Maps.newHashMap();
        this.project = system.add(PROJECT_ID, TypeTag.of(IProject.class),
                self -> new ProjectActor<>(self, new UnitContext(self), root, edgeLabels));
        project.addMonitor(system.async(dlm));
    }

    @Override public void add(String id, ITypeChecker<S, L, D, ?> unitChecker) {
        final IActor<IUnit<S, L, D>> unit = system.add(id, TypeTag.of(IUnit.class),
                self -> new UnitActor<>(self, project, new UnitContext(self), unitChecker, root, edgeLabels));
        unit.addMonitor(system.async(dlm));
        units.put(id, unit);
    }

    @Override public IFuture<IResult<S, L, D>> run(ICancel cancel) {
        system.start();
        system.async(project)._start(units.values());
        for(IActor<IUnit<S, L, D>> unit : units.values()) {
            system.async(unit)._start();
        }

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

    private class UnitContext implements IUnitContext<S, L, D> {

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
            final String id = scopeImpl.id(scope);
            return id.equals(PROJECT_ID) ? project : units.get(id);
        }

        @Override public void waitForInit(IActorRef<? extends IUnit2UnitProtocol<S, L, D>> unit, S root) {
            // TODO Implement method IUnitContext<S,L,D>::waitForInit.
            throw new UnsupportedOperationException("Method IUnitContext<S,L,D>::waitForInit not implemented.");
        }

        @Override public void grantedInit(IActorRef<? extends IUnit2UnitProtocol<S, L, D>> unit, S root) {
            // TODO Implement method IUnitContext<S,L,D>::grantedInit.
            throw new UnsupportedOperationException("Method IUnitContext<S,L,D>::grantedInit not implemented.");
        }

        @Override public void waitForClose(IActorRef<? extends IUnit2UnitProtocol<S, L, D>> unit, S scope,
                Iterable<L> labels) {
            // TODO Implement method IUnitContext<S,L,D>::waitForClose.
            throw new UnsupportedOperationException("Method IUnitContext<S,L,D>::waitForClose not implemented.");
        }

        @Override public void grantedClose(IActorRef<? extends IUnit2UnitProtocol<S, L, D>> unit, S scope, L label) {
            // TODO Implement method IUnitContext<S,L,D>::grantedClose.
            throw new UnsupportedOperationException("Method IUnitContext<S,L,D>::grantedClose not implemented.");
        }

        @Override public void waitForAnswer(IActorRef<? extends IUnit2UnitProtocol<S, L, D>> unit, IFuture<?> future) {
            // TODO Implement method IUnitContext<S,L,D>::waitForAnswer.
            throw new UnsupportedOperationException("Method IUnitContext<S,L,D>::waitForAnswer not implemented.");
        }

        @Override public void grantedAnswer(IActorRef<? extends IUnit2UnitProtocol<S, L, D>> unit, IFuture<?> future) {
            // TODO Implement method IUnitContext<S,L,D>::grantedAnswer.
            throw new UnsupportedOperationException("Method IUnitContext<S,L,D>::grantedAnswer not implemented.");
        }


    }

}