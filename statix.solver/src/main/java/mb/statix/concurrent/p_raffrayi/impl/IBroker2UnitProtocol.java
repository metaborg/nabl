package mb.statix.concurrent.p_raffrayi.impl;

import javax.annotation.Nullable;

import mb.statix.concurrent.actors.futures.IFuture;
import mb.statix.concurrent.p_raffrayi.IUnitResult;

public interface IBroker2UnitProtocol<S, L, D, R> {

    void _start(@Nullable S root);

    IFuture<IUnitResult<S, L, D, R>> _done();

    void _fail();

}