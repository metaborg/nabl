package mb.statix.concurrent.p_raffrayi.impl;

import javax.annotation.Nullable;

import io.usethesource.capsule.SetMultimap;
import mb.statix.concurrent.actors.IActorRef;
import mb.statix.concurrent.actors.deadlock.Clock;
import mb.statix.concurrent.actors.futures.IFuture;
import mb.statix.concurrent.p_raffrayi.IUnitResult;
import mb.statix.concurrent.p_raffrayi.impl.tokens.IWaitFor;

public interface IBroker2UnitProtocol<S, L, D, R> {

    IFuture<IUnitResult<S, L, D, R>> _start(@Nullable S root);

    void _deadlocked(Clock<IActorRef<? extends IUnit<S, L, D, R>>> clock,
            java.util.Set<IActorRef<? extends IUnit<S, L, D, R>>> nodes,
            SetMultimap.Immutable<IActorRef<? extends IUnit<S, L, D, R>>, IWaitFor<S, L, D>> waitFors);

}