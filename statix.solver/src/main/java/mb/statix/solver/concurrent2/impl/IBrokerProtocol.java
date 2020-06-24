package mb.statix.solver.concurrent2.impl;

/**
 * Protocol accepted by the broker, from units
 */
public interface IBrokerProtocol {

    IUnitProtocol get(String id);

    void suspend();

}