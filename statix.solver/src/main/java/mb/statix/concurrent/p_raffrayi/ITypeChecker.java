package mb.statix.concurrent.p_raffrayi;

import mb.statix.concurrent.actors.futures.IFuture;

/**
 * Represents the user-implemented type checker for a specific unit.
 */
@FunctionalInterface
public interface ITypeChecker<S, L, D, R> {

    IFuture<R> run(ITypeCheckerContext<S, L, D, R> unit, S root);

}