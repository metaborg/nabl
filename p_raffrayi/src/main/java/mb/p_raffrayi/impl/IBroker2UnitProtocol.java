package mb.p_raffrayi.impl;

import java.util.List;

import org.metaborg.util.future.IFuture;

import mb.p_raffrayi.IUnitResult;

public interface IBroker2UnitProtocol<S, L, D, R, T> {

    IFuture<IUnitResult<S, L, D, R, T>> _start(List<S> rootScopes);

}