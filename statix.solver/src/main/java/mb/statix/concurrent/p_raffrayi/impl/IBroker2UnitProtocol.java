package mb.statix.concurrent.p_raffrayi.impl;

import java.util.List;

import mb.statix.concurrent.actors.futures.IFuture;
import mb.statix.concurrent.p_raffrayi.IUnitResult;

public interface IBroker2UnitProtocol<S, L, D, R> {

    IFuture<IUnitResult<S, L, D, R>> _start(List<S> rootScopes);

}