package mb.statix.concurrent.p_raffrayi.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import org.metaborg.util.log.ILogger;
import org.metaborg.util.log.LoggerUtils;
import org.metaborg.util.unit.Unit;

import mb.nabl2.util.Tuple2;
import mb.statix.concurrent.actors.IActor;
import mb.statix.concurrent.actors.IActorRef;
import mb.statix.concurrent.actors.futures.CompletableFuture;
import mb.statix.concurrent.actors.futures.IFuture;
import mb.statix.concurrent.p_raffrayi.IScopeGraphLibrary;
import mb.statix.concurrent.p_raffrayi.IUnitResult;
import mb.statix.concurrent.p_raffrayi.impl.tokens.Query;
import mb.statix.concurrent.p_raffrayi.nameresolution.DataLeq;
import mb.statix.concurrent.p_raffrayi.nameresolution.DataWf;
import mb.statix.concurrent.p_raffrayi.nameresolution.LabelOrder;
import mb.statix.concurrent.p_raffrayi.nameresolution.LabelWf;
import mb.statix.scopegraph.IScopeGraph;
import mb.statix.scopegraph.reference.EdgeOrData;
import mb.statix.scopegraph.reference.Env;
import mb.statix.scopegraph.terms.newPath.ScopePath;

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
        throw new UnsupportedOperationException("Not supported by static scope graph units.");
    }

    ///////////////////////////////////////////////////////////////////////////
    // IBroker2UnitProtocol interface, called by IBroker implementations
    ///////////////////////////////////////////////////////////////////////////

    @Override public IFuture<IUnitResult<S, L, D, Unit>> _start(List<S> rootScopes) {
        doStart(rootScopes);
        buildScopeGraph(rootScopes);
        clearLibrary();
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
        final Tuple2<? extends Set<S>, IScopeGraph.Immutable<S, L, D>> libraryResult =
                library.initialize(rootScopes, this::makeScope);
        this.scopes.__insertAll(libraryResult._1());
        this.scopeGraph.set(libraryResult._2());

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
                    }, Collections.emptyList());
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

    @Override public IFuture<Env<S, L, D>> _query(ScopePath<S, L> path, LabelWf<L> labelWF, DataWf<S, L, D> dataWF,
            LabelOrder<L> labelOrder, DataLeq<S, L, D> dataEquiv) {
        stats.incomingQueries += 1;
        final IActorRef<? extends IUnit<S, L, D, Unit>> worker = workers.get(stats.incomingQueries % workers.size());

        final IFuture<Env<S, L, D>> result = self.async(worker)._query(path, labelWF, dataWF, labelOrder, dataEquiv);
        final Query<S, L, D> token = Query.of(self, path, labelWF, dataWF, labelOrder, dataEquiv, result);
        waitFor(token, worker);
        return result.whenComplete((r, ex) -> {
            granted(token, worker);
        });
    }

    ///////////////////////////////////////////////////////////////////////////
    // Worker
    ///////////////////////////////////////////////////////////////////////////


    ///////////////////////////////////////////////////////////////////////////
    // toString
    ///////////////////////////////////////////////////////////////////////////

    @Override public String toString() {
        return "StaticScopeGraphUnit{" + self.id() + "}";
    }

}