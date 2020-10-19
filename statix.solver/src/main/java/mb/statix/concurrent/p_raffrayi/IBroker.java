package mb.statix.concurrent.p_raffrayi;

import mb.statix.concurrent.actors.futures.IFuture;

/**
 * Represents the whole system of type checkers to the outside.
 */
public interface IBroker<S, L, D, R> {

    void add(String id, ITypeChecker<S, L, D, R> unitChecker);

    void run();

    IFuture<IBrokerResult<S, L, D, R>> result();

}