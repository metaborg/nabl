package mb.statix.solver.concurrent2.impl;

import java.util.Set;

import mb.statix.actors.IActorRef;
import mb.statix.actors.IFuture;
import mb.statix.scopegraph.path.IResolutionPath;

/**
 * Protocol accepted by the broker, from units
 */
public interface IUnitContext<S, L, D> {

    S makeScope(String baseName);

    IActorRef<? extends IUnitProtocol<S, L, D>> owner(S scope);

    //////////////////////////////////////////////////////////////////////////
    // Deadlock detection
    //////////////////////////////////////////////////////////////////////////

    void waitForInit(IActorRef<? extends IUnitProtocol<S, L, D>> unit, S root);

    void grantedInit(IActorRef<? extends IUnitProtocol<S, L, D>> unit, S root);

    void waitForClose(IActorRef<? extends IUnitProtocol<S, L, D>> unit, S scope, Iterable<L> labels);

    void grantedClose(IActorRef<? extends IUnitProtocol<S, L, D>> unit, S scope, L label);

    void waitForAnswer(IActorRef<? extends IUnitProtocol<S, L, D>> unit, IFuture<Set<IResolutionPath<S, L, D>>> future);

    void grantedAnswer(IActorRef<? extends IUnitProtocol<S, L, D>> unit, IFuture<Set<IResolutionPath<S, L, D>>> future);

}