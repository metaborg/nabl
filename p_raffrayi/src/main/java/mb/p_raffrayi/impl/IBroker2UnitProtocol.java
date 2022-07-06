package mb.p_raffrayi.impl;

import java.util.List;

import org.metaborg.util.future.IFuture;

import mb.p_raffrayi.IUnitResult;

public interface IBroker2UnitProtocol<S, L, D, R> {

    IFuture<IUnitResult<S, L, D, R>> _start(List<S> rootScopes);

}
