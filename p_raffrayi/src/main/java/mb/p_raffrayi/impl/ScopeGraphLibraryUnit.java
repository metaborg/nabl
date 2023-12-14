package mb.p_raffrayi.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.annotation.Nullable;

import org.metaborg.util.future.CompletableFuture;
import org.metaborg.util.future.ICompletableFuture;
import org.metaborg.util.future.IFuture;
import org.metaborg.util.log.ILogger;
import org.metaborg.util.log.LoggerUtils;
import org.metaborg.util.tuple.Tuple2;
import org.metaborg.util.unit.Unit;

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
import mb.scopegraph.library.IScopeGraphLibrary;
import mb.scopegraph.oopsla20.IScopeGraph;
import mb.scopegraph.oopsla20.reference.EdgeOrData;
import mb.scopegraph.oopsla20.terms.newPath.ScopePath;
import mb.scopegraph.patching.IPatchCollection;
import mb.scopegraph.patching.PatchCollection;
import mb.scopegraph.patching.Patcher;
import mb.scopegraph.patching.Patcher.DataPatchCallback;

class ScopeGraphLibraryUnit<S, L, D> extends AbstractUnit<S, L, D, Unit> {

    private static final ILogger logger = LoggerUtils.logger(ScopeGraphLibraryUnit.class);

    private IScopeGraphLibrary<S, L, D> library;

    private final @Nullable IUnitResult<S, L, D, ?> previousResult;

    private final List<IActorRef<? extends IUnit<S, L, D, Unit>>> workers;

    ScopeGraphLibraryUnit(IActor<? extends IUnit<S, L, D, Unit>> self,
            @Nullable IActorRef<? extends IUnit<S, L, D, ?>> parent, IUnitContext<S, L, D> context,
            Iterable<L> edgeLabels, IScopeGraphLibrary<S, L, D> library,
            @Nullable IUnitResult<S, L, D, ?> previousResult) {
        super(self, parent, context, edgeLabels);

        // these are replaced once started
        this.library = library;
        this.workers = new ArrayList<>();

        this.previousResult = previousResult;
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
        if(previousResult == null) {
            buildScopeGraph(rootScopes);
        } else {
            restoreScopeGraph(rootScopes);
        }
        clearLibrary();
        if(isDifferEnabled() && previousResult != null) {
            initDiffer(new MatchingDiffer<>(differOps(), differContext(d -> d)), rootScopes,
                    previousResult.rootScopes());
        }
        startWorkers();

        final ICompletableFuture<Unit> validationFinished = new CompletableFuture<>();
        validateScopeGraph(validationFinished);
        return doFinish(validationFinished);
    }

    private void buildScopeGraph(List<S> rootScopes) {
        final long t0 = System.currentTimeMillis();

        initRootScopes(rootScopes);

        // initialize library
        // Using context::makeScope assumes unique names in library
        // and deterministic generation of scopes.
        // Required to make diffs/matches deterministic.
        final Tuple2<? extends Set<S>, IScopeGraph.Immutable<S, L, D>> libraryResult =
                library.initialize(rootScopes, context::makeScope);
        this.scopes.__insertAll(libraryResult._1());
        scopeGraph.set(libraryResult._2());

        closeRootScopes(rootScopes);

        final long dt = System.currentTimeMillis() - t0;
        logger.info("Initialized {} in {} ms", self.id(), dt);
    }

    private void restoreScopeGraph(List<S> rootScopes) {
        initRootScopes(rootScopes);

        final IPatchCollection.Transient<S> patches = PatchCollection.Transient.of();
        final Iterator<S> previousScopes = previousResult.rootScopes().iterator();
        for(S currentScope : rootScopes) {
            patches.put(currentScope, previousScopes.next());
        }

        if(patches.isIdentity()) {
            scopeGraph.set(previousResult.scopeGraph());
        } else {
            // @formatter:off
            final Patcher<S, L, D> patcher = new Patcher.Builder<S, L, D>()
                .patchSources(patches).patchEdgeTargets(patches)
                .patchDatumSources(patches).patchDatums(patches, context::substituteScopes)
                .build();
            scopeGraph.set(patcher.apply(previousResult.scopeGraph(),
                (s, t) -> Unit.unit,
                (s_o, s_n, l, t_o, t_n, u) -> { },
                DataPatchCallback.noop()
            ));
            // @formatter:on
        }
        this.scopes.__insertAll(previousResult.scopes());

        closeRootScopes(rootScopes);
    }

    public void initRootScopes(List<S> rootScopes) {
        final List<EdgeOrData<L>> edges = edgeLabels.stream().map(EdgeOrData::edge).collect(Collectors.toList());

        // initialize root scopes
        for(S rootScope : rootScopes) {
            doInitShare(self, rootScope, edges, false);
        }
    }

    public void closeRootScopes(List<S> rootScopes) {
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

    private void validateScopeGraph(ICompletableFuture<Unit> finished) {
        final ICompletableFuture<Unit> future = new CompletableFuture<>();
        future.thenApply(unit -> {
            final Set<L> invalidLabels = new HashSet<>();
            for(L label: scopeGraph.get().getLabels()) {
                if(!edgeLabels.contains(label)) {
                    invalidLabels.add(label);
                }
            }
            if(!invalidLabels.isEmpty()) {
                logger.warn("Scope graph library contains labels not in type checker specification: {}.", invalidLabels);
            }
            return Unit.unit;
        }).whenComplete(finished::complete);

        // Asynchronous validation to prevent it from becoming performance bottleneck
        self.complete(future, Unit.unit, null);
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
        final IActorRef<? extends IUnit<S, L, D, Unit>> worker = workers.get(stats.incomingQueries % workers.size());

        final IFuture<IQueryAnswer<S, L, D>> result = self.async(worker)._query(origin, path, query, dataWF, dataEquiv);
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

    @Override public IFuture<ConfirmResult<S, L, D>> _confirm(S scope, LabelWf<L> labelWF, DataWf<S, L, D> dataWF,
            boolean prevEnvEmpty) {
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
        throw new UnsupportedOperationException("Not supported by static scope graph units.");
    }

    ///////////////////////////////////////////////////////////////////////////
    // toString
    ///////////////////////////////////////////////////////////////////////////

    @Override public String toString() {
        return "StaticScopeGraphUnit{" + self.id() + "}";
    }

}
