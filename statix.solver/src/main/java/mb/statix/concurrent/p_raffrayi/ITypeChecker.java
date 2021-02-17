package mb.statix.concurrent.p_raffrayi;

import java.util.List;

import javax.annotation.Nullable;

import mb.statix.concurrent.actors.futures.CompletableFuture;
import mb.statix.concurrent.actors.futures.IFuture;

/**
 * Represents the user-implemented type checker for a specific unit.
 */
public interface ITypeChecker<S, L, D, R> {

    IFuture<R> run(ITypeCheckerContext<S, L, D> unit, List<S> rootScopes, @Nullable IUnitResult<S, L, D, R> previousResult);

    default IFuture<R> run(ITypeCheckerContext<S, L, D> unit, List<S> rootScopes) {
    	return run(unit, rootScopes, null);
    }

    default IFuture<D> getExternalDatum(D datum) {
        return CompletableFuture.completedFuture(datum);
    }

}