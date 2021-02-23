package mb.statix.concurrent.p_raffrayi.confirmation;

import io.usethesource.capsule.Map;
import mb.nabl2.util.Tuple2;
import mb.statix.concurrent.actors.futures.CompletableFuture;
import mb.statix.concurrent.actors.futures.IFuture;
import mb.statix.concurrent.p_raffrayi.IRecordedQuery;

public class DenyingConfirmation<S, L, D> implements IQueryConfirmation<S, L, D> {

    @Override public IFuture<Tuple2<Boolean, Map.Immutable<S, S>>> confirm(IRecordedQuery<S, L, D> query) {
        return CompletableFuture.completedFuture(Tuple2.of(false, Map.Immutable.of()));
    }

}
