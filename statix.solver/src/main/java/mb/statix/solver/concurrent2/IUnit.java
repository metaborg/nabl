package mb.statix.solver.concurrent2;

import mb.statix.solver.concurrent2.impl.IUnitProtocol;

public interface IUnit<S, L, D> extends IUnitProtocol<S, L, D>, IClientProtocol<S, L, D> {

    void deadlocked();

}