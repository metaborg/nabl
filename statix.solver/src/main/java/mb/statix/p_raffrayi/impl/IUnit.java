package mb.statix.p_raffrayi.impl;

import mb.statix.p_raffrayi.ITypeCheckerContext;

public interface IUnit<S, L, D>
        extends IUnit2UnitProtocol<S, L, D>, IBroker2UnitProtocol<S, L, D>, ITypeCheckerContext<S, L, D> {

}