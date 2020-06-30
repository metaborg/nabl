package mb.statix.solver.concurrent2.impl;

import mb.statix.solver.concurrent2.ITypeCheckerContext;

public interface IUnit<S, L, D>
        extends IUnit2UnitProtocol<S, L, D>, IBroker2UnitProtocol<S, L, D>, ITypeCheckerContext<S, L, D> {

}