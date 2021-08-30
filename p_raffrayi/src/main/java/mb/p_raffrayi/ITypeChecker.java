package mb.p_raffrayi;

import java.util.List;

import javax.annotation.Nullable;

import org.metaborg.util.future.CompletableFuture;
import org.metaborg.util.future.IFuture;

/**
 * Represents the user-implemented type checker for a specific unit.
 */
public interface ITypeChecker<S, L, D, R extends IResult<S, L, D>, T extends ITypeCheckerState<S, L, D>> {

    // FIXME: include local snapshot T in future result instead of AbstractUnit?
    IFuture<R> run(IIncrementalTypeCheckerContext<S, L, D, R, T> unit, List<S> rootScopes);

    default IFuture<D> getExternalDatum(D datum) {
        return CompletableFuture.completedFuture(datum);
    }

    default D internalData(D datum) {
        return datum;
    }

    default @Nullable T snapshot() {
        return null;
    }

}