package mb.p_raffrayi;

import java.util.List;

import org.metaborg.util.future.CompletableFuture;
import org.metaborg.util.future.IFuture;

/**
 * Represents the user-implemented type checker for a specific unit.
 */
public interface ITypeChecker<S, L, D, R> {

    IFuture<R> run(ITypeCheckerContext<S, L, D> unit, List<S> rootScopes);

    default IFuture<D> getExternalDatum(D datum) {
        return CompletableFuture.completedFuture(datum);
    }

}