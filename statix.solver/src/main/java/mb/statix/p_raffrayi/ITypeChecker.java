package mb.statix.p_raffrayi;

import mb.statix.actors.futures.IFuture;

/**
 * Represents the user-implemented type checker for a specific unit.
 */
@FunctionalInterface
public interface ITypeChecker<S, L, D, R> {

    IFuture<R> run(ITypeCheckerContext<S, L, D, R> unit, S root) throws InterruptedException;

}