package mb.p_raffrayi.impl;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.metaborg.util.future.CompletableFuture;
import org.metaborg.util.future.IFuture;
import org.metaborg.util.log.ILogger;
import org.metaborg.util.log.LoggerUtils;
import org.metaborg.util.unit.Unit;

import mb.p_raffrayi.IUnitResult;
import mb.p_raffrayi.actors.IActor;
import mb.p_raffrayi.actors.IActorRef;
import mb.p_raffrayi.impl.diff.IScopeGraphDiffer;
import mb.p_raffrayi.impl.diff.IDifferScopeOps;
import mb.p_raffrayi.impl.diff.MatchingDiffer;
import mb.p_raffrayi.nameresolution.DataLeq;
import mb.p_raffrayi.nameresolution.DataWf;
import mb.scopegraph.ecoop21.LabelOrder;
import mb.scopegraph.ecoop21.LabelWf;
import mb.scopegraph.oopsla20.IScopeGraph.Immutable;
import mb.scopegraph.oopsla20.diff.BiMap;
import mb.scopegraph.oopsla20.reference.EdgeOrData;
import mb.scopegraph.oopsla20.terms.newPath.ScopePath;

class ScopeGraphLibraryWorker<S, L, D> extends AbstractUnit<S, L, D, Unit> {

    private static final ILogger logger = LoggerUtils.logger(ScopeGraphLibraryWorker.class);

    private final IDifferScopeOps<S, D> scopeOps;

    ScopeGraphLibraryWorker(IActor<? extends IUnit<S, L, D, Unit>> self, IActorRef<? extends IUnit<S, L, D, ?>> parent,
            IUnitContext<S, L, D> context, Iterable<L> edgeLabels, Set<S> scopes, Immutable<S, L, D> scopeGraph,
            IDifferScopeOps<S, D> scopeOps) {
        super(self, parent, context, edgeLabels);

        this.scopes.__insertAll(scopes);
        this.scopeGraph.set(scopeGraph);

        this.scopeOps = scopeOps;
    }

    @Override protected IFuture<D> getExternalDatum(D datum) {
        return CompletableFuture.completedFuture(datum);
    }

    ///////////////////////////////////////////////////////////////////////////
    // IBroker2UnitProtocol interface, called by IBroker implementations
    ///////////////////////////////////////////////////////////////////////////

    @Override public IFuture<IUnitResult<S, L, D, Unit>> _start(List<S> rootScopes) {
        doStart(rootScopes, Collections.emptyList());
        return doFinish(CompletableFuture.completedFuture(Unit.unit));
    }

    @SuppressWarnings("unused") @Override public void _initShare(S scope, Iterable<EdgeOrData<L>> edges,
            boolean sharing) {
        throw new UnsupportedOperationException("Not supported by static scope graph units.");
    }

    @SuppressWarnings("unused") @Override public void _addShare(S scope) {
        throw new UnsupportedOperationException("Not supported by static scope graph units.");
    }

    @SuppressWarnings("unused") @Override public void _doneSharing(S scope) {
        throw new UnsupportedOperationException("Not supported by static scope graph units.");
    }

    @SuppressWarnings("unused") @Override public void _addEdge(S source, L label, S target) {
        throw new UnsupportedOperationException("Not supported by static scope graph units.");
    }

    @SuppressWarnings("unused") @Override public void _closeEdge(S scope, EdgeOrData<L> edge) {
        throw new UnsupportedOperationException("Not supported by static scope graph units.");
    }


    @Override public IFuture<IQueryAnswer<S, L, D>> _query(ScopePath<S, L> path, LabelWf<L> labelWF,
            DataWf<S, L, D> dataWF, LabelOrder<L> labelOrder, DataLeq<S, L, D> dataEquiv) {
        // duplicate of AbstractUnit::_query
        // resume(); // FIXME necessary?
        stats.incomingQueries += 1;
        return doQuery(self.sender(TYPE), path, labelWF, labelOrder, dataWF, dataEquiv, null, null);
    }

    @Override public IFuture<ReleaseOrRestart<S>> _requireRestart() {
        return CompletableFuture.completedFuture(ReleaseOrRestart.release());
    }

    @Override public void _release(BiMap.Immutable<S> patches) {
        throw new UnsupportedOperationException("Not supported by static scope graph units.");
    }

    @Override public void _restart() {
        // As part of the DataWf and DataLeq params of incoming queries, library workers can have outgoing queries,
        // originating from data{WF,LEq} parameters.
        // When these cause a deadlock, workers can receive a restart.
    }

    @Override protected IScopeGraphDiffer<S, L, D> initDiffer() {
        return new MatchingDiffer<>(new DifferOps(scopeOps));
    }

    @Override protected boolean canAnswer(S scope) {
        return context.scopeId(scope).equals(parent.id());
    }

    @Override protected void assertOwnScope(S scope) {
        if(!context.scopeId(scope).equals(parent.id())) {
            logger.error("Scope {} is not owned {}", scope, this);
            throw new IllegalArgumentException("Scope " + scope + " is not owned " + this);
        }
    }

}