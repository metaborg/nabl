package mb.p_raffrayi;

import java.util.List;

import mb.p_raffrayi.impl.AInitialState;
import mb.p_raffrayi.impl.IInitialState;
import org.metaborg.util.future.CompletableFuture;
import org.metaborg.util.future.IFuture;

/**
 * Represents the user-implemented type checker for a specific unit.
 */
public interface ITypeChecker<S, L, D, R> {

    IFuture<R> run(IIncrementalTypeCheckerContext<S, L, D, R> unit, List<S> rootScopes, IInitialState<S, L, D, R> initialState);

    // TODO remove
    default IFuture<R> run(IIncrementalTypeCheckerContext<S, L, D, R> unit, List<S> rootScopes) {
    	return run(unit, rootScopes, AInitialState.added());
    }

    default IFuture<D> getExternalDatum(D datum) {
        return CompletableFuture.completedFuture(datum);
    }

}