package mb.statix.p_raffrayi.impl;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.metaborg.util.functions.Function2;
import org.metaborg.util.task.ICancel;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Streams;

import mb.statix.actors.futures.AggregateFuture;
import mb.statix.actors.futures.CompletableFuture;
import mb.statix.actors.futures.IFuture;
import mb.statix.scopegraph.IScopeGraph;
import mb.statix.scopegraph.path.IResolutionPath;
import mb.statix.scopegraph.path.IScopePath;
import mb.statix.scopegraph.reference.Access;
import mb.statix.scopegraph.reference.DataLeq;
import mb.statix.scopegraph.reference.DataWF;
import mb.statix.scopegraph.reference.EdgeOrData;
import mb.statix.scopegraph.reference.Env;
import mb.statix.scopegraph.reference.LabelOrder;
import mb.statix.scopegraph.reference.LabelWF;
import mb.statix.scopegraph.reference.ResolutionException;
import mb.statix.scopegraph.terms.path.Paths;

abstract class NameResolution<S, L, D> {

    private final IScopeGraph<S, L, D> scopeGraph;

    private final EdgeOrData<L> dataLabel;
    private final Set<EdgeOrData<L>> allLabels;

    private final LabelOrder<L> labelOrder; // default: false

    private final DataWF<D> dataWF; // default: true
    private final DataLeq<D> dataEquiv; // default: false

    private final Function2<S, EdgeOrData<L>, IFuture<Void>> isComplete; // default: true

    public NameResolution(IScopeGraph<S, L, D> scopeGraph, LabelOrder<L> labelOrder, DataWF<D> dataWF,
            DataLeq<D> dataEquiv, Function2<S, EdgeOrData<L>, IFuture<Void>> isComplete) {
        this.scopeGraph = scopeGraph;
        this.dataLabel = EdgeOrData.data(Access.INTERNAL);
        this.allLabels = Streams.concat(Stream.of(dataLabel), scopeGraph.getEdgeLabels().stream().map(EdgeOrData::edge))
                .collect(Collectors.toSet());
        this.labelOrder = labelOrder;
        this.dataWF = dataWF;
        this.dataEquiv = dataEquiv;
        this.isComplete = isComplete;
    }

    public abstract Optional<IFuture<Env<S, L, D>>> externalEnv(IScopePath<S, L> path, LabelWF<L> re,
            LabelOrder<L> labelOrder, DataWF<D> dataWF, DataLeq<D> dataEquiv);

    public IFuture<Env<S, L, D>> env(IScopePath<S, L> path, LabelWF<L> re, ICancel cancel) {
        return externalEnv(path, re, labelOrder, dataWF, dataEquiv).orElseGet(() -> env_L(path, re, allLabels, cancel));
    }

    // FIXME Use caching of single label environments to prevent recalculation in case of diamonds in
    // the graph
    private IFuture<Env<S, L, D>> env_L(IScopePath<S, L> path, LabelWF<L> re, Set<EdgeOrData<L>> L, ICancel cancel) {
        try {
            cancel.throwIfCancelled();
            final Set<EdgeOrData<L>> max_L = max(L);
            final List<IFuture<Env<S, L, D>>> envs = Lists.newArrayList();
            for(EdgeOrData<L> l : max_L) {
                final IFuture<Env<S, L, D>> env1 = env_L(path, re, smaller(L, l), cancel);
                envs.add(env1);
                final IFuture<Env<S, L, D>> env2 = env1.thenCompose((e1) -> {
                    if(!e1.isEmpty() && dataEquiv.alwaysTrue()) {
                        return empty();
                    }
                    return env_l(path, re, l, cancel).thenApply(e2 -> {
                        return minus(e2, e1);
                    });
                });
                envs.add(env2);
            }
            return new AggregateFuture<>(envs).thenApply((es) -> {
                final Env.Builder<S, L, D> env = Env.builder();
                es.forEach(env::addAll);
                return env.build();
            });
        } catch(ResolutionException | InterruptedException ex) {
            return CompletableFuture.completedExceptionally(ex);
        }
    }

    private Set<EdgeOrData<L>> max(Set<EdgeOrData<L>> L) throws ResolutionException, InterruptedException {
        final ImmutableSet.Builder<EdgeOrData<L>> max = ImmutableSet.builder();
        outer: for(EdgeOrData<L> l1 : L) {
            for(EdgeOrData<L> l2 : L) {
                if(labelOrder.lt(l1, l2)) {
                    continue outer;
                }
            }
            max.add(l1);
        }
        return max.build();
    }

    private Set<EdgeOrData<L>> smaller(Set<EdgeOrData<L>> L, EdgeOrData<L> l1)
            throws ResolutionException, InterruptedException {
        final ImmutableSet.Builder<EdgeOrData<L>> smaller = ImmutableSet.builder();
        for(EdgeOrData<L> l2 : L) {
            if(labelOrder.lt(l2, l1)) {
                smaller.add(l2);
            }
        }
        return smaller.build();
    }

    private IFuture<Env<S, L, D>> env_l(IScopePath<S, L> path, LabelWF<L> re, EdgeOrData<L> l, ICancel cancel) {
        try {
            return l.matchInResolution(acc -> env_data(path, re), lbl -> env_edges(path, re, lbl, cancel));
        } catch(ResolutionException | InterruptedException e) {
            throw new IllegalStateException("Should not happen.");
        }
    }

    private IFuture<Env<S, L, D>> env_data(IScopePath<S, L> path, LabelWF<L> re) {
        try {
            if(!re.accepting()) {
                return empty();
            }
            return isComplete.apply(path.getTarget(), dataLabel).thenApply(ignored -> {
                final D datum;
                if((datum = getData(re, path).orElse(null)) == null || !dataWF.wf(datum)) {
                    return Env.empty();
                }
                return Env.of(Paths.resolve(path, datum));
            });
        } catch(ResolutionException | InterruptedException ex) {
            return CompletableFuture.completedExceptionally(ex);
        }
    }

    private IFuture<Env<S, L, D>> env_edges(IScopePath<S, L> path, LabelWF<L> re, L l, ICancel cancel) {
        try {
            final LabelWF<L> newRe;
            if((newRe = re.step(l).orElse(null)) == null) {
                return empty();
            }
            final EdgeOrData<L> edgeLabel = EdgeOrData.edge(l);
            return isComplete.apply(path.getTarget(), edgeLabel).thenCompose(ignored -> {
                List<IFuture<Env<S, L, D>>> envs = Lists.newArrayList();
                for(S nextScope : getEdges(newRe, path, l)) {
                    final Optional<IScopePath<S, L>> p = Paths.append(path, Paths.edge(path.getTarget(), l, nextScope));
                    if(p.isPresent()) {
                        envs.add(env(p.get(), newRe, cancel));
                    }
                }
                return new AggregateFuture<>(envs).thenApply(es -> {
                    final Env.Builder<S, L, D> env = Env.builder();
                    es.forEach(env::addAll);
                    return env.build();
                });
            });
        } catch(ResolutionException | InterruptedException ex) {
            return CompletableFuture.completedExceptionally(ex);
        }
    }

    ///////////////////////////////////////////////////////////////////////////
    // environments                                                          //
    ///////////////////////////////////////////////////////////////////////////

    private Env<S, L, D> minus(Env<S, L, D> env1, Env<S, L, D> env2) throws ResolutionException, InterruptedException {
        final Env.Builder<S, L, D> env = Env.builder();
        outer: for(IResolutionPath<S, L, D> p1 : env1) {
            for(IResolutionPath<S, L, D> p2 : env2) {
                if(dataEquiv.leq(p2.getDatum(), p1.getDatum())) {
                    continue outer;
                }
            }
            env.add(p1);
        }
        return env.build();
    }

    private IFuture<Env<S, L, D>> empty() {
        return CompletableFuture.of(Env.empty());
    }

    ///////////////////////////////////////////////////////////////////////////
    // edges and data                                                        //
    ///////////////////////////////////////////////////////////////////////////

    protected Optional<D> getData(LabelWF<L> re, IScopePath<S, L> path) {
        return scopeGraph.getData(path.getTarget());
    }

    protected Iterable<S> getEdges(LabelWF<L> re, IScopePath<S, L> path, L l) {
        return scopeGraph.getEdges(path.getTarget(), l);
    }

}