package mb.p_raffrayi.impl.confirm;

import java.util.Optional;

import org.metaborg.util.future.Futures;
import org.metaborg.util.future.IFuture;
import org.metaborg.util.future.AggregateFuture.SC;

import mb.p_raffrayi.IRecordedQuery;
import mb.p_raffrayi.impl.envdiff.AddedEdge;
import mb.p_raffrayi.impl.envdiff.External;
import mb.p_raffrayi.impl.envdiff.RemovedEdge;
import mb.p_raffrayi.nameresolution.DataLeq;
import mb.scopegraph.ecoop21.LabelOrder;
import mb.scopegraph.oopsla20.diff.BiMap;
import mb.scopegraph.oopsla20.terms.newPath.ScopePath;

public class LazyConfirmation<S, L, D> extends BaseConfirmation<S, L, D> {

    public LazyConfirmation(IConfirmationContext<S, L, D> context) {
        super(context);
    }

    @Override public IFuture<Optional<BiMap.Immutable<S>>> confirm(IRecordedQuery<S, L, D> query) {
        // First confirm root query
        return confirmSingle(query).thenCompose(res -> {
            return Futures.<BiMap.Immutable<S>>liftOptional(res.map(initialPatches -> {
                // If that succeeds, confirm all transitive queries
                return confirm(query.transitiveQueries()).thenCompose(res2 -> {
                    return Futures.<BiMap.Immutable<S>>liftOptional(res2.map(transitivePatches -> {
                        // If that succeeds, confirm all queries raised by predicates
                        return confirm(query.predicateQueries()).thenApply(res3 -> {
                            return res3.map(__ -> {
                                // Do no include patches from predicate queries in eventual result, because
                                // nested state should not leak to the outer state, and hence plays no role in confirmation.
                                return initialPatches.putAll(transitivePatches);
                            });
                        });
                    }));
                });
            }));
        });
    }

    @Override protected IFuture<SC<? extends BiMap.Immutable<S>, ? extends Optional<BiMap.Immutable<S>>>>
            handleAddedEdge(AddedEdge<S, L, D> addedEdge) {
        return context.query(new ScopePath<>(addedEdge.scope()), addedEdge.labelWf(), LabelOrder.none(),
                addedEdge.dataWf(), DataLeq.none()).thenApply(ans -> ans.env().isEmpty() ? accept() : deny());
    }

    @Override protected IFuture<SC<? extends BiMap.Immutable<S>, ? extends Optional<BiMap.Immutable<S>>>>
            handleRemovedEdge(RemovedEdge<S, L, D> removedEdge, boolean prevEnvEnpty) {
        if(prevEnvEnpty) {
            return acceptFuture();
        }
        return context.queryPrevious(new ScopePath<>(removedEdge.scope()), removedEdge.labelWf(), removedEdge.dataWf(),
                LabelOrder.none(), DataLeq.none()).thenApply(env -> env.isEmpty() ? accept() : deny());
    }

    @Override protected IFuture<SC<? extends BiMap.Immutable<S>, ? extends Optional<BiMap.Immutable<S>>>>
            handleExternal(External<S, L, D> external) {
        // External env diff validated by transitively recorded query
        return acceptFuture();
    }

    public static <S, L, D> IConfirmationFactory<S, L, D> factory() {
        return LazyConfirmation::new;
    }

}