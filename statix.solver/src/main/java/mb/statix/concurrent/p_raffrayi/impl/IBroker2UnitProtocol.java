package mb.statix.concurrent.p_raffrayi.impl;

import javax.annotation.Nullable;

import mb.statix.concurrent.actors.IActorRef;
import mb.statix.concurrent.actors.deadlock.Clock;
import mb.statix.concurrent.actors.futures.IFuture;
import mb.statix.concurrent.p_raffrayi.IUnitResult;

public interface IBroker2UnitProtocol<S, L, D, R> {

    IFuture<IUnitResult<S, L, D, R>> _start(@Nullable S root);

    void _deadlocked(Clock<IActorRef<? extends IUnit<S, L, D, ?>>> clock,
            java.util.Set<IActorRef<? extends IUnit<S, L, D, ?>>> nodes);

}