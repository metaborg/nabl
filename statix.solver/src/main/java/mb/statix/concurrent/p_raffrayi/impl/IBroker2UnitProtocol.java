package mb.statix.concurrent.p_raffrayi.impl;

import javax.annotation.Nullable;

import mb.statix.concurrent.actors.futures.IFuture;
import mb.statix.concurrent.p_raffrayi.IUnitResult;
import mb.statix.concurrent.p_raffrayi.impl.tokens.IWaitFor;

public interface IBroker2UnitProtocol<S, L, D, R> {

    IFuture<IUnitResult<S, L, D, R>> _start(@Nullable S root);

    void _done();

    void _deadlocked(Iterable<IWaitFor<S, L, D>> waitFors);

}