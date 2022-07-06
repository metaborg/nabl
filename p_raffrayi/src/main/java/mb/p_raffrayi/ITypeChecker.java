package mb.p_raffrayi;

import java.util.List;
import java.util.Optional;

import org.metaborg.util.future.CompletableFuture;
import org.metaborg.util.future.IFuture;

/**
 * Represents the user-implemented type checker for a specific unit.
 */
public interface ITypeChecker<S, L, D, R extends ITypeChecker.IOutput<S, L, D>, T extends ITypeChecker.IState<S, L, D>> {

    IFuture<R> run(IIncrementalTypeCheckerContext<S, L, D, R, T> unit, List<S> rootScopes);

    default IFuture<D> getExternalDatum(D datum) {
        return CompletableFuture.completedFuture(datum);
    }

    default D internalData(D datum) {
        return datum;
    }

    default T snapshot() {
        throw new UnsupportedOperationException("This type-checker cannot be used in incremental mode.");
    }

    public interface IOutput<S, L, D> {

        D getExternalRepresentation(D datum);

    }

    public interface IState<S, L, D> {

        Optional<D> tryGetExternalDatum(D datum);

    }


}
