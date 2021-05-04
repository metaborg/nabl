package mb.p_raffrayi.impl.confirmation;

import org.metaborg.util.future.CompletableFuture;
import org.metaborg.util.future.IFuture;
import org.metaborg.util.tuple.Tuple2;

import io.usethesource.capsule.Map;
import mb.p_raffrayi.IRecordedQuery;

public class DenyingConfirmation<S, L, D> implements IQueryConfirmation<S, L, D> {

    @Override public IFuture<Tuple2<Boolean, Map.Immutable<S, S>>> confirm(IRecordedQuery<S, L, D> query) {
        return CompletableFuture.completedFuture(Tuple2.of(false, Map.Immutable.of()));
    }

}
