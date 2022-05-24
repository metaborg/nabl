package mb.p_raffrayi.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import org.metaborg.util.future.CompletableFuture;
import org.metaborg.util.future.IFuture;
import org.metaborg.util.log.ILogger;
import org.metaborg.util.log.LoggerUtils;
import org.metaborg.util.tuple.Tuple2;
import org.metaborg.util.unit.Unit;

import mb.p_raffrayi.IScopeGraphLibrary;
import mb.p_raffrayi.IUnitResult;
import mb.p_raffrayi.actors.IActor;
import mb.p_raffrayi.actors.IActorRef;
import mb.p_raffrayi.impl.confirm.ConfirmResult;
import mb.p_raffrayi.impl.diff.MatchingDiffer;
import mb.p_raffrayi.impl.tokens.Query;
import mb.p_raffrayi.nameresolution.DataLeq;
import mb.p_raffrayi.nameresolution.DataWf;
import mb.p_raffrayi.nameresolution.IQuery;
import mb.scopegraph.ecoop21.LabelWf;
import mb.scopegraph.oopsla20.IScopeGraph;
import mb.scopegraph.oopsla20.reference.EdgeOrData;
import mb.scopegraph.oopsla20.terms.newPath.ScopePath;

class ScopeGraphLibraryUnit<S, L, D> extends AbstractUnit<S, L, D, Unit> {

    private static final ILogger logger = LoggerUtils.logger(ScopeGraphLibraryUnit.class);

    private IScopeGraphLibrary<S, L, D> library;

    private final List<IActorRef<? extends IUnit<S, L, D, Unit>>> workers;

    ScopeGraphLibraryUnit(IActor<? extends IUnit<S, L, D, Unit>> self,
            @Nullable IActorRef<? extends IUnit<S, L, D, ?>> parent, IUnitContext<S, L, D> context,
            Iterable<L> edgeLabels, IScopeGraphLibrary<S, L, D> library) {
        super(self, parent, context, edgeLabels);

        // these are replaced once started
        this.library = library;
        this.workers = new ArrayList<>();
    }

    protected void clearLibrary() {
        this.library = null;
    }

    @SuppressWarnings("unused") @Override protected IFuture<D> getExternalDatum(D datum) {
        return CompletableFuture.completedFuture(datum);
    }

    @Override protected D getPreviousDatum(D datum) {
        throw new UnsupportedOperationException("Not supported by static scope graph units.");
    }

    ///////////////////////////////////////////////////////////////////////////
    // IBroker2UnitProtocol interface, called by IBroker implementations
    ///////////////////////////////////////////////////////////////////////////

    @Override public IFuture<IUnitResult<S, L, D, Unit>> _start(List<S> rootScopes) {
        doStart(rootScopes);
        buildScopeGraph(rootScopes);
        clearLibrary();
        if(isDifferEnabled()) {
            initDiffer(new MatchingDiffer<>(differOps(), differContext(d -> d)), Collections.emptyList(),
                    Collections.emptyList());
        }
        startWorkers();
        return doFinish(CompletableFuture.completedFuture(Unit.unit));
    }

    private void buildScopeGraph(List<S> rootScopes) {
        final long t0 = System.currentTimeMillis();

        final List<EdgeOrData<L>> edges = edgeLabels.stream().map(EdgeOrData::edge).collect(Collectors.toList());

        // initialize root scopes
        for(S rootScope : rootScopes) {
            doInitShare(self, rootScope, edges, false);
        }

        // initialize library
        // Using context::makeScope assumes unique names in library
        // and deterministic generation of scopes.
        // Required to make diffs/matches deterministic.
        final Tuple2<? extends Set<S>, IScopeGraph.Immutable<S, L, D>> libraryResult =
                library.initialize(rootScopes, context::makeScope);
        this.scopes.__insertAll(libraryResult._1());
        scopeGraph.set(libraryResult._2());

        // add root scope edges and close root scopes
        for(S rootScope : rootScopes) {
            for(L label : edgeLabels) {
                final EdgeOrData<L> l = EdgeOrData.edge(label);
                for(S target : scopeGraph.get().getEdges(rootScope, label)) {
                    doAddEdge(self, rootScope, label, target);
                }
                doCloseLabel(self, rootScope, l);
            }
        }

        final long dt = System.currentTimeMillis() - t0;
        logger.info("Initialized {} in {} s", self.id(), TimeUnit.SECONDS.convert(dt, TimeUnit.MILLISECONDS));
    }

    private void startWorkers() {
        for(int i = 0; i < context.parallelism(); i++) {
            final Tuple2<IActorRef<? extends IUnit<S, L, D, Unit>>, IFuture<IUnitResult<S, L, D, Unit>>> worker =
                    doAddSubUnit("worker-" + i, (subself, subcontext) -> {
                        return new ScopeGraphLibraryWorker<>(subself, self, subcontext, edgeLabels, scopes,
                                scopeGraph.get());
                    }, Collections.emptyList(), true);
            workers.add(worker._1());
        }
    }

    ///////////////////////////////////////////////////////////////////////////
    // IUnit2UnitProtocol interface, called by IUnit implementations
    ///////////////////////////////////////////////////////////////////////////

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

    @Override public IFuture<IQueryAnswer<S, L, D>> _query(IActorRef<? extends IUnit<S, L, D, ?>> origin,
            ScopePath<S, L> path, IQuery<S, L, D> query, DataWf<S, L, D> dataWF, DataLeq<S, L, D> dataEquiv) {
        stats.incomingQueries += 1;
        final IActorRef<? extends IUnit<S, L, D, Unit>> worker =
                workers.get(stats.incomingQueries % workers.size());

        final IFuture<IQueryAnswer<S, L, D>> result =
                self.async(worker)._query(origin, path, query, dataWF, dataEquiv);
        final Query<S, L, D> token = Query.of(self, path, query, dataWF, dataEquiv, result);
        waitFor(token, worker);
        return result.whenComplete((r, ex) -> {
            granted(token, worker);
            resume();
        });
    }

    @Override public IFuture<IQueryAnswer<S, L, D>> _queryPrevious(ScopePath<S, L> path, IQuery<S, L, D> query,
            DataWf<S, L, D> dataWF, DataLeq<S, L, D> dataEquiv) {
        return _query(self.sender(TYPE), path, query, dataWF, dataEquiv);
    }

    @Override public IFuture<ConfirmResult<S, L, D>> _confirm(ScopePath<S, L> path, LabelWf<L> labelWF,
            DataWf<S, L, D> dataWF, boolean prevEnvEmpty) {
        return CompletableFuture.completedFuture(ConfirmResult.confirm());
    }

    @Override public IFuture<Optional<S>> _match(S previousScope) {
        // Assume libraries are static, and an update to a library requires a clean run.
        return CompletableFuture.completedFuture(Optional.of(previousScope));
    }

    @Override public IFuture<StateSummary<S, L, D>> _state() {
        return CompletableFuture.completedFuture(StateSummary.released(process, dependentSet()));
    }

    @Override public void _release() {
        throw new UnsupportedOperationException("Not supported by static scope graph units.");
    }

    @Override public void _restart() {
        // As part of the DataWf and DataLeq params of incoming queries, library the workers of a library unit
        // can have outgoing queries. When these cause a deadlock, workers can receive a restart.
    }

    ///////////////////////////////////////////////////////////////////////////
    // toString
    ///////////////////////////////////////////////////////////////////////////

    @Override public String toString() {
        return "StaticScopeGraphUnit{" + self.id() + "}";
    }

}
