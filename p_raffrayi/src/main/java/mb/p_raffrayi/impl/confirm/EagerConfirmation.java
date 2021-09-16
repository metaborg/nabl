package mb.p_raffrayi.impl.confirm;

import java.util.ArrayList;
import java.util.List;

import org.metaborg.util.future.AggregateFuture;
import org.metaborg.util.future.AggregateFuture.SC;
import org.metaborg.util.future.IFuture;

import mb.p_raffrayi.IRecordedQuery;
import mb.scopegraph.oopsla20.diff.BiMap;

public class EagerConfirmation<S, L, D> extends OptimisticConfirmation<S, L, D> {

    public EagerConfirmation(IConfirmationContext<S, L, D> context) {
        super(context);
    }

    @Override public IFuture<ConfirmResult<S>> confirm(IRecordedQuery<S, L, D> query) {
        final int size = 1 + query.transitiveQueries().size() + query.predicateQueries().size();
        final List<IFuture<SC<BiMap.Immutable<S>, ConfirmResult<S>>>> futures = new ArrayList<>(size);
        
        futures.add(toSCFuture(confirmSingle(query)));
        query.transitiveQueries().forEach(q -> futures.add(toSCFuture(confirmSingle(q))));
        query.predicateQueries().forEach(q -> futures.add(toSCFuture(confirmSingle(q))));

        return AggregateFuture.ofShortCircuitable(this::merge, futures);
    }

    public static <S, L, D> IConfirmationFactory<S, L, D> factory() {
        return EagerConfirmation::new;
    }

}
