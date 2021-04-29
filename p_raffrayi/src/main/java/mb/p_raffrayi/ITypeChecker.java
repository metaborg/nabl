package mb.p_raffrayi;

import java.util.List;

import org.metaborg.util.future.CompletableFuture;
import org.metaborg.util.future.IFuture;

import mb.p_raffrayi.nameresolution.DataLeq;
import mb.p_raffrayi.nameresolution.DataWf;

/**
 * Represents the user-implemented type checker for a specific unit.
 */
public interface ITypeChecker<S, L, D, R, St> {

    IFuture<R> run(ITypeCheckerContext<S, L, D> unit, List<S> rootScopes);

    default IFuture<D> getExternalDatum(D datum) {
        return CompletableFuture.completedFuture(datum);
    }

    // Snapshot taking/query stats validation

    St stateSnapshot();

    DataWf<S, L, D> dataWf(St state);

    DataLeq<S, L, D> dataLeq(St state);

}