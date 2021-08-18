package mb.p_raffrayi.impl;

import mb.p_raffrayi.IResult;

public interface IUnit<S, L, D, R extends IResult<S, L, D>, T>
        extends IUnit2UnitProtocol<S, L, D>, IBroker2UnitProtocol<S, L, D, R, T>, IDeadlockProtocol<S, L, D> {

}