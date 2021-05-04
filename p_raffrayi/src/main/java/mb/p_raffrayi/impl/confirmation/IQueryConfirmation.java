package mb.p_raffrayi.impl.confirmation;

import org.metaborg.util.future.IFuture;
import org.metaborg.util.tuple.Tuple2;

import io.usethesource.capsule.Map;
import mb.p_raffrayi.IRecordedQuery;

public interface IQueryConfirmation<S, L, D> {
    /**
     * Confirms a query.
     *
     * @return A tuple containing a boolean that is true when the result is confirmed,
     * and a map that contains the scope patches as (oldname, newname) pairs.
     */
    IFuture<Tuple2<Boolean, Map.Immutable<S, S>>> confirm(IRecordedQuery<S, L, D> query);

}
