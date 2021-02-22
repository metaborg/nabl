package mb.statix.concurrent.p_raffrayi.confirmation;

import io.usethesource.capsule.Map;
import mb.nabl2.util.Tuple2;
import mb.statix.concurrent.actors.futures.IFuture;
import mb.statix.concurrent.p_raffrayi.IRecordedQuery;

public interface IQueryConfirmation<S, L, D> {
    /**
     * Confirms a query.
     * 
     * @return A tuple containing a boolean that is true when the result is confirmed,
     * and a map that contains the scope patches as (oldname, newname) pairs.
     */
    IFuture<Tuple2<Boolean, Map<S, S>>> confirm(IRecordedQuery<S, L, D> query);

}
