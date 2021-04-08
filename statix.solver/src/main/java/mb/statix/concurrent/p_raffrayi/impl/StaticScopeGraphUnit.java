package mb.statix.concurrent.p_raffrayi.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import org.metaborg.util.log.ILogger;
import org.metaborg.util.log.LoggerUtils;
import org.metaborg.util.unit.Unit;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

import mb.nabl2.util.Tuple2;
import mb.statix.concurrent.actors.IActor;
import mb.statix.concurrent.actors.IActorRef;
import mb.statix.concurrent.actors.futures.CompletableFuture;
import mb.statix.concurrent.actors.futures.IFuture;
import mb.statix.concurrent.p_raffrayi.IUnitResult;
import mb.statix.concurrent.p_raffrayi.impl.tokens.Query;
import mb.statix.concurrent.p_raffrayi.nameresolution.DataLeq;
import mb.statix.concurrent.p_raffrayi.nameresolution.DataWf;
import mb.statix.concurrent.p_raffrayi.nameresolution.LabelOrder;
import mb.statix.concurrent.p_raffrayi.nameresolution.LabelWf;
import mb.statix.scopegraph.IScopeGraph;
import mb.statix.scopegraph.IScopeGraph.Immutable;
import mb.statix.scopegraph.reference.EdgeOrData;
import mb.statix.scopegraph.reference.Env;
import mb.statix.scopegraph.terms.newPath.ScopePath;

class StaticScopeGraphUnit<S, L, D> extends AbstractUnit<S, L, D, Unit> {

    private static final ILogger logger = LoggerUtils.logger(StaticScopeGraphUnit.class);

    protected List<S> givenRootScopes;
    protected Set<S> givenOwnScopes;
    protected IScopeGraph.Immutable<S, EdgeOrEps<L>, D> givenScopeGraph;

    private final List<IActorRef<? extends IUnit<S, L, D, Unit>>> workers;

    StaticScopeGraphUnit(IActor<? extends IUnit<S, L, D, Unit>> self,
            @Nullable IActorRef<? extends IUnit<S, L, D, ?>> parent, IUnitContext<S, L, D> context,
            Iterable<L> edgeLabels, List<S> givenRootScopes, Set<S> givenOwnScopes,
            IScopeGraph.Immutable<S, EdgeOrEps<L>, D> givenScopeGraph) {
        super(self, parent, context, edgeLabels);

        // these are replaced once started
        this.givenRootScopes = ImmutableList.copyOf(givenRootScopes);
        this.givenOwnScopes = ImmutableSet.copyOf(givenOwnScopes);
        this.givenScopeGraph = givenScopeGraph;

        this.workers = new ArrayList<>();
    }

    protected void clearGiven() {
        this.givenRootScopes = null;
        this.givenOwnScopes = null;
        this.givenScopeGraph = null;
    }

    @SuppressWarnings("unused") @Override protected IFuture<D> getExternalDatum(D datum) {
        throw new UnsupportedOperationException("Not supported by static scope graph units.");
    }

    ///////////////////////////////////////////////////////////////////////////
    // IBroker2UnitProtocol interface, called by IBroker implementations
    ///////////////////////////////////////////////////////////////////////////

    @Override public IFuture<IUnitResult<S, L, D, Unit>> _start(List<S> rootScopes) {
        doStart(rootScopes);

        final Set<S> ownScopes = buildScopeGraph(rootScopes);

        clearGiven();

        startWorkers(rootScopes, ownScopes);

        return doFinish(CompletableFuture.completedFuture(Unit.unit));
    }

    private Set<S> buildScopeGraph(List<S> rootScopes) {
        final long t0 = System.currentTimeMillis();

        final List<EdgeOrData<L>> edges = edgeLabels.stream().map(EdgeOrData::edge).collect(Collectors.toList());

        final Map<S, S> scopeMap = new HashMap<>();
        final Set<S> ownScopes = new HashSet<>();

        // map given scopes to actual scopes

        if(givenRootScopes.size() != rootScopes.size()) {
            throw new IllegalArgumentException("Number of root scopes does not match.");
        }
        for(int i = 0; i < rootScopes.size(); i++) {
            final S libRootScope = givenRootScopes.get(i);
            if(scopeMap.containsKey(libRootScope)) {
                continue;
            }
            final S rootScope = rootScopes.get(i);
            scopeMap.put(libRootScope, rootScope);
            doInitShare(self, rootScope, edges, false);
        }
        for(S libScope : givenOwnScopes) {
            if(scopeMap.containsKey(libScope)) {
                throw new IllegalStateException("Scope already initialized.");
            }
            final S scope = makeScope("s"/*libScope.getName()*/);
            ownScopes.add(scope);
            scopeMap.put(libScope, scope);
        }

        // add data and edges to actual scopes

        for(S libScope : givenRootScopes) {
            final S scope = scopeMap.get(libScope);

            for(L label : edgeLabels) {
                final EdgeOrData<L> l = EdgeOrData.edge(label);
                for(S libTarget : givenScopeGraph.getEdges(libScope, EdgeOrEps.edge(label))) {
                    final S target = scopeMap.get(libTarget);
                    doAddEdge(self, scope, label, target);
                }
                doCloseLabel(self, scope, l);
            }
        }
        for(S libScope : givenOwnScopes) {
            final S scope = scopeMap.get(libScope);

            final D libDatum;
            if((libDatum = givenScopeGraph.getData(libScope).orElse(null)) != null) {
                final D datum = context.substituteScopes(libDatum, scopeMap);
                scopeGraph.set(scopeGraph.get().setDatum(scope, datum));
            }

            for(L label : edgeLabels) {
                for(S libTarget : givenScopeGraph.getEdges(libScope, EdgeOrEps.edge(label))) {
                    final S target = scopeMap.get(libTarget);
                    scopeGraph.set(scopeGraph.get().addEdge(scope, EdgeOrEps.edge(label), target));
                }
            }
        }

        final long dt = System.currentTimeMillis() - t0;
        logger.info("Initialized {} in {} s", self.id(), TimeUnit.SECONDS.convert(dt, TimeUnit.MILLISECONDS));

        return ownScopes;
    }

    private void startWorkers(List<S> rootScopes, final Set<S> ownScopes) {
        for(int i = 0; i < context.parallelism(); i++) {
            final Tuple2<IActorRef<? extends IUnit<S, L, D, Unit>>, IFuture<IUnitResult<S, L, D, Unit>>> worker =
                    doAddSubUnit("worker-" + i, (subself, subcontext) -> {
                        return new StaticScopeGraphWorker<>(subself, self, subcontext, edgeLabels, rootScopes,
                                ownScopes, scopeGraph.get());
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

    private static class StaticScopeGraphWorker<S, L, D> extends StaticScopeGraphUnit<S, L, D> {

        StaticScopeGraphWorker(IActor<? extends IUnit<S, L, D, Unit>> self,
                IActorRef<? extends IUnit<S, L, D, ?>> parent, IUnitContext<S, L, D> context, Iterable<L> edgeLabels,
                List<S> rootScopes, Set<S> ownScopes, Immutable<S, EdgeOrEps<L>, D> scopeGraph) {
            super(self, parent, context, edgeLabels, rootScopes, ownScopes, scopeGraph);
        }

        @Override protected IFuture<D> getExternalDatum(D datum) {
            return CompletableFuture.completedFuture(datum);
        }

        @Override public IFuture<IUnitResult<S, L, D, Unit>> _start(List<S> rootScopes) {
            doStart(rootScopes);

            this.givenRootScopes.forEach(scopes::__insert);
            this.givenOwnScopes.forEach(scopes::__insert);
            this.scopeGraph.set(this.givenScopeGraph);

            clearGiven();

            return doFinish(CompletableFuture.completedFuture(Unit.unit));
        }

        @Override public IFuture<Env<S, L, D>> _query(ScopePath<S, L> path, LabelWf<L> labelWF, DataWf<S, L, D> dataWF,
                LabelOrder<L> labelOrder, DataLeq<S, L, D> dataEquiv) {
            // duplicate of AbstractUnit::_query
            // resume(); // FIXME necessary?
            stats.incomingQueries += 1;
            return doQuery(self.sender(TYPE), path, labelWF, labelOrder, dataWF, dataEquiv, null, null);
        }

        @Override protected boolean canAnswer(S scope) {
            final IActorRef<? extends IUnit<S, L, D, ?>> owner = context.owner(scope);
            return owner.equals(parent);
        }

    }

    ///////////////////////////////////////////////////////////////////////////
    // toString
    ///////////////////////////////////////////////////////////////////////////

    @Override public String toString() {
        return "StaticScopeGraphUnit{" + self.id() + "}";
    }

}