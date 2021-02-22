package mb.statix.concurrent.p_raffrayi;

import java.util.List;

import mb.statix.concurrent.actors.futures.CompletableFuture;
import mb.statix.concurrent.actors.futures.IFuture;
import mb.statix.concurrent.p_raffrayi.impl.AInitialState;
import mb.statix.concurrent.p_raffrayi.impl.IInitialState;

/**
 * Represents the user-implemented type checker for a specific unit.
 */
public interface ITypeChecker<S, L, D, R> {

    IFuture<R> run(ITypeCheckerContext<S, L, D> unit, List<S> rootScopes, IInitialState<S, L, D, R> initialState);

    // TODO remove
    default IFuture<R> run(ITypeCheckerContext<S, L, D> unit, List<S> rootScopes) {
    	return run(unit, rootScopes, AInitialState.added());
    }

    default IFuture<D> getExternalDatum(D datum) {
        return CompletableFuture.completedFuture(datum);
    }

}