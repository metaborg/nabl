package mb.statix.concurrent.p_raffrayi.impl;

import mb.statix.concurrent.p_raffrayi.ITypeCheckerContext;

public interface IUnit<S, L, D, R>
        extends IUnit2UnitProtocol<S, L, D, R>, IBroker2UnitProtocol<S, L, D, R>, ITypeCheckerContext<S, L, D, R> {

}