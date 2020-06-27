package mb.statix.solver.concurrent2.impl;

import mb.statix.solver.concurrent2.IScopeImpl;

/**
 * Protocol accepted by the broker, from units
 */
public interface IBrokerProtocol<S, L, D> {

    String id();

    IUnitProtocol<S, L, D> get(String id);

    IScopeImpl<S> scopeImpl();

    void fail();

}