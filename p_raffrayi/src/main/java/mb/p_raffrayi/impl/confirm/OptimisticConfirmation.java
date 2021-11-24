package mb.p_raffrayi.impl.confirm;

import org.metaborg.util.future.AggregateFuture.SC;
import org.metaborg.util.log.ILogger;
import org.metaborg.util.log.LoggerUtils;
import org.metaborg.util.future.IFuture;

import mb.p_raffrayi.impl.envdiff.AddedEdge;
import mb.p_raffrayi.impl.envdiff.RemovedEdge;
import mb.p_raffrayi.nameresolution.DataLeq;
import mb.p_raffrayi.nameresolution.DataWf;
import mb.scopegraph.ecoop21.LabelOrder;
import mb.scopegraph.oopsla20.diff.BiMap;
import mb.scopegraph.oopsla20.terms.newPath.ScopePath;

public abstract class OptimisticConfirmation<S, L, D> extends BaseConfirmation<S, L, D> {

    private static final ILogger logger = LoggerUtils.logger(OptimisticConfirmation.class);

    public OptimisticConfirmation(IConfirmationContext<S, L, D> context) {
        super(context);
    }

    @Override protected IFuture<SC<BiMap.Immutable<S>, ConfirmResult<S>>> handleAddedEdge(AddedEdge<S, L, D> addedEdge,
            DataWf<S, L, D> dataWf) {
        logger.debug("Handling added edge by regular query: {}.", addedEdge);
        // TODO: use path prefix to prevent false positives on cyclic edges
        return context.query(new ScopePath<>(addedEdge.target()), addedEdge.labelWf(), LabelOrder.none(), dataWf,
                DataLeq.none()).thenApply(ans -> ans.env().isEmpty() ? accept() : deny());
    }

    @Override protected IFuture<SC<BiMap.Immutable<S>, ConfirmResult<S>>>
            handleRemovedEdge(RemovedEdge<S, L, D> removedEdge, DataWf<S, L, D> dataWf, boolean prevEnvEnpty) {
        if(prevEnvEnpty) {
            logger.debug("Confirming removed edge: previous environment empty.");
            return acceptFuture();
        }
        logger.debug("Confirming removed edge by previous result query: {}.", removedEdge);
        // TODO: use path prefix to prevent false positives on cyclic edges
        return context.queryPrevious(new ScopePath<>(removedEdge.target()), removedEdge.labelWf(), dataWf,
                LabelOrder.none(), DataLeq.none()).thenApply(env -> env.isEmpty() ? accept() : deny());
    }

}
