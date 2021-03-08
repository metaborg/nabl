package mb.statix.concurrent.p_raffrayi.impl;

import mb.statix.concurrent.p_raffrayi.IIncrementalTypeCheckerContext;

public interface IUnit<S, L, D, R>
        extends IUnit2UnitProtocol<S, L, D, R>, IBroker2UnitProtocol<S, L, D, R>, IIncrementalTypeCheckerContext<S, L, D, R> {

}