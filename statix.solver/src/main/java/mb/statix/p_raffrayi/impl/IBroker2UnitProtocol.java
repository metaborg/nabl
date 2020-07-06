package mb.statix.p_raffrayi.impl;

import javax.annotation.Nullable;

import mb.statix.actors.futures.IFuture;
import mb.statix.p_raffrayi.IUnitResult;

public interface IBroker2UnitProtocol<S, L, D, R> {

    IFuture<IUnitResult<S, L, D, R>> _start(@Nullable S root);

}