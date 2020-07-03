package mb.statix.p_raffrayi.impl;

import org.metaborg.util.task.ICancel;

import mb.statix.actors.IActorRef;
import mb.statix.p_raffrayi.ITypeChecker;

/**
 * Protocol accepted by the broker, from units
 */
public interface IUnitContext<S, L, D, R> {

    ICancel cancel();

    S makeScope(String name);

    IActorRef<? extends IUnit2UnitProtocol<S, L, D>> owner(S scope);

    IActorRef<? extends IUnit2UnitProtocol<S, L, D>> add(String id, ITypeChecker<S, L, D, R> unitChecker, S root);

    //////////////////////////////////////////////////////////////////////////
    // Deadlock detection
    //////////////////////////////////////////////////////////////////////////

    void suspend(Clock<S, L, D> clock);

    void waitFor(IWaitFor token, IActorRef<? extends IUnit2UnitProtocol<S, L, D>> unit);

    void granted(IWaitFor token, IActorRef<? extends IUnit2UnitProtocol<S, L, D>> unit);

}