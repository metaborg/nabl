package mb.statix.solver.concurrent2.impl;

import org.metaborg.util.task.ICancel;

import mb.statix.actors.IActorRef;
import mb.statix.actors.futures.IFuture;

/**
 * Protocol accepted by the broker, from units
 */
public interface IUnitContext<S, L, D> {

    ICancel cancel();

    S makeScope(String name);

    IActorRef<? extends IUnit2UnitProtocol<S, L, D>> owner(S scope);

    //////////////////////////////////////////////////////////////////////////
    // Deadlock detection
    //////////////////////////////////////////////////////////////////////////

    void waitForInit(IActorRef<? extends IUnit2UnitProtocol<S, L, D>> unit, S root);

    void grantedInit(IActorRef<? extends IUnit2UnitProtocol<S, L, D>> unit, S root);

    void waitForClose(IActorRef<? extends IUnit2UnitProtocol<S, L, D>> unit, S scope, Iterable<L> labels);

    void grantedClose(IActorRef<? extends IUnit2UnitProtocol<S, L, D>> unit, S scope, L label);

    void waitForAnswer(IActorRef<? extends IUnit2UnitProtocol<S, L, D>> unit, IFuture<?> future);

    void grantedAnswer(IActorRef<? extends IUnit2UnitProtocol<S, L, D>> unit, IFuture<?> future);

}