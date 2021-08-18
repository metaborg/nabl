package mb.p_raffrayi.impl;

public interface IUnit<S, L, D, R, T>
        extends IUnit2UnitProtocol<S, L, D>, IBroker2UnitProtocol<S, L, D, R, T>, IDeadlockProtocol<S, L, D> {

}