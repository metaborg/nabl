package mb.p_raffrayi.impl.confirm;

import org.metaborg.util.future.AggregateFuture.SC;
import org.metaborg.util.log.ILogger;
import org.metaborg.util.log.LoggerUtils;
import org.metaborg.util.future.IFuture;

import mb.p_raffrayi.impl.envdiff.AddedEdge;
import mb.p_raffrayi.impl.envdiff.External;
import mb.p_raffrayi.impl.envdiff.RemovedEdge;
import mb.p_raffrayi.nameresolution.DataLeq;
import mb.scopegraph.ecoop21.LabelOrder;
import mb.scopegraph.oopsla20.diff.BiMap;
import mb.scopegraph.oopsla20.terms.newPath.ScopePath;

public abstract class OptimisticConfirmation<S, L, D> extends BaseConfirmation<S, L, D> {

    private static final ILogger logger = LoggerUtils.logger(OptimisticConfirmation.class);
    
    public OptimisticConfirmation(IConfirmationContext<S, L, D> context) {
        super(context);
    }

    @Override protected IFuture<SC<BiMap.Immutable<S>, ConfirmResult<S>>> handleAddedEdge(AddedEdge<S, L, D> addedEdge) {
        logger.debug("Handling added edge by regular query: {}.", addedEdge);
        return context.query(new ScopePath<>(addedEdge.scope()), addedEdge.labelWf(), LabelOrder.none(),
                addedEdge.dataWf(), DataLeq.none()).thenApply(ans -> ans.env().isEmpty() ? accept() : deny());
    }

    @Override protected IFuture<SC<BiMap.Immutable<S>, ConfirmResult<S>>> handleRemovedEdge(RemovedEdge<S, L, D> removedEdge, boolean prevEnvEnpty) {
        if(prevEnvEnpty) {
            logger.debug("Confirming removed edge: previous environment empty.");
            return acceptFuture();
        }
        logger.debug("Confirming removed edge by previous result query: {}.", removedEdge);
        return context.queryPrevious(new ScopePath<>(removedEdge.scope()), removedEdge.labelWf(), removedEdge.dataWf(),
                LabelOrder.none(), DataLeq.none()).thenApply(env -> env.isEmpty() ? accept() : deny());
    }

    @Override protected IFuture<SC<BiMap.Immutable<S>, ConfirmResult<S>>> handleExternal(External<S, L, D> external) {
        // External env diff validated by transitively recorded query
        logger.debug("Trivially accepting external env diff.");
        return acceptFuture();
    }

}