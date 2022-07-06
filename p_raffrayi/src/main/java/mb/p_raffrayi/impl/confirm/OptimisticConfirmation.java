package mb.p_raffrayi.impl.confirm;

import java.util.Optional;

import org.metaborg.util.future.AggregateFuture.SC;
import org.metaborg.util.log.ILogger;
import org.metaborg.util.log.LoggerUtils;
import org.metaborg.util.future.IFuture;

import mb.p_raffrayi.impl.envdiff.AddedEdge;
import mb.p_raffrayi.impl.envdiff.RemovedEdge;
import mb.p_raffrayi.nameresolution.DataLeq;
import mb.p_raffrayi.nameresolution.DataWf;
import mb.scopegraph.ecoop21.LabelOrder;
import mb.scopegraph.oopsla20.terms.newPath.ScopePath;

public class OptimisticConfirmation<S, L, D> extends BaseConfirmation<S, L, D> {

    private static final ILogger logger = LoggerUtils.logger(OptimisticConfirmation.class);

    public OptimisticConfirmation(IConfirmationContext<S, L, D> context) {
        super(context);
    }

    @Override protected IFuture<SC<ConfirmResult<S, L, D>, ConfirmResult<S, L, D>>>
            handleAddedEdge(AddedEdge<S, L, D> addedEdge, LazyFuture<Optional<DataWf<S, L, D>>> dataWf) {
        logger.debug("Handling added edge by regular query: {}.", addedEdge);
        // TODO: use path prefix to prevent false positives on cyclic edges
        return dataWf.get().thenCompose(newDataWfOpt -> {
            return newDataWfOpt.map(newDataWf -> {
                return context.query(new ScopePath<>(addedEdge.target()), addedEdge.labelWf(), LabelOrder.none(),
                        newDataWf, DataLeq.none()).thenApply(ans -> ans.env().isEmpty() ? acceptAdded(ans.transitiveQueries(), ans.predicateQueries()) : deny());
            }).orElse(denyFuture());
        });
    }

    @Override protected IFuture<SC<ConfirmResult<S, L, D>, ConfirmResult<S, L, D>>>
            handleRemovedEdge(RemovedEdge<S, L, D> removedEdge, DataWf<S, L, D> dataWf, boolean prevEnvEnpty) {
        if(prevEnvEnpty) {
            logger.debug("Confirming removed edge: previous environment empty.");
            return acceptFuture();
        }
        logger.debug("Confirming removed edge by previous result query: {}.", removedEdge);
        // TODO: use path prefix to prevent false positives on cyclic edges
        return context.queryPrevious(new ScopePath<>(removedEdge.target()), removedEdge.labelWf(), dataWf,
                LabelOrder.none(), DataLeq.none()).thenApply(ans -> ans.env().isEmpty() ? acceptRemoved(ans.transitiveQueries(), ans.predicateQueries()) : deny());
    }

    public static <S, L, D> IConfirmationFactory<S, L, D> factory() {
        return OptimisticConfirmation::new;
    }

}
