package mb.p_raffrayi.impl;

public interface IUnit<S, L, D, R>
        extends IUnit2UnitProtocol<S, L, D>, IBroker2UnitProtocol<S, L, D, R>, IDeadlockProtocol<S, L, D> {

}