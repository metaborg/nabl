package mb.statix.concurrent.p_raffrayi.impl;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.metaborg.util.Ref;
import org.metaborg.util.functions.Function2;
import org.metaborg.util.log.ILogger;
import org.metaborg.util.log.LoggerUtils;
import org.metaborg.util.task.ICancel;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Streams;

import mb.statix.concurrent.actors.futures.AggregateFuture;
import mb.statix.concurrent.actors.futures.CompletableFuture;
import mb.statix.concurrent.actors.futures.IFuture;
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

    private static final ILogger logger = LoggerUtils.logger(NameResolution.class);

    private final Ref<? extends IScopeGraph<S, L, D>> scopeGraph;

    private final EdgeOrData<L> dataLabel;
    private final Set<EdgeOrData<L>> allLabels;

    private final LabelOrder<L> labelOrder; // default: false

    private final DataWF<D> dataWF; // default: true
    private final DataLeq<D> dataEquiv; // default: false

    private final Function2<S, EdgeOrData<L>, IFuture<Void>> isComplete; // default: true

    public NameResolution(Ref<? extends IScopeGraph<S, L, D>> scopeGraph, LabelOrder<L> labelOrder, DataWF<D> dataWF,
            DataLeq<D> dataEquiv, Access access, Function2<S, EdgeOrData<L>, IFuture<Void>> isComplete) {
        this.scopeGraph = scopeGraph;
        this.dataLabel = EdgeOrData.data(access);
        this.allLabels =
                Streams.concat(Stream.of(dataLabel), scopeGraph.get().getEdgeLabels().stream().map(EdgeOrData::edge))
                        .collect(Collectors.toSet());
        this.labelOrder = labelOrder;
        this.dataWF = dataWF;
        this.dataEquiv = dataEquiv;
        this.isComplete = isComplete;
    }

    public abstract Optional<IFuture<Env<S, L, D>>> externalEnv(IScopePath<S, L> path, LabelWF<L> re,
            LabelOrder<L> labelOrder, DataWF<D> dataWF, DataLeq<D> dataEquiv);

    public IFuture<Env<S, L, D>> env(IScopePath<S, L> path, LabelWF<L> re, ICancel cancel) {
        logger.trace("env {}", path);
        return externalEnv(path, re, labelOrder, dataWF, dataEquiv).orElseGet(() -> env_L(path, re, allLabels, cancel));
    }

    // FIXME Use caching of single label environments to prevent recalculation in case of diamonds in
    // the graph
    private IFuture<Env<S, L, D>> env_L(IScopePath<S, L> path, LabelWF<L> re, Set<EdgeOrData<L>> L, ICancel cancel) {
        logger.trace("env_L {} {} {}", path, re, L);
        try {
            cancel.throwIfCancelled();
            final Set<EdgeOrData<L>> max_L = max(L);
            final List<IFuture<Env<S, L, D>>> envs = Lists.newArrayList();
            for(EdgeOrData<L> l : max_L) {
                final IFuture<Env<S, L, D>> env1 = env_L(path, re, smaller(L, l), cancel);
                logger.trace("env_L {} {} {}: env1: {}", path, re, L, env1);
                env1.whenComplete((r, ex) -> logger.trace("env_L {} {} {}: result1: {}", path, re, L, env1));
                envs.add(env1);
                final IFuture<Env<S, L, D>> env2 = env1.thenCompose((e1) -> {
                    if(!e1.isEmpty() && dataEquiv.alwaysTrue()) {
                        return empty();
                    }
                    return env_l(path, re, l, cancel).thenApply(e2 -> {
                        return minus(e2, e1);
                    });
                });
                logger.trace("env_L {} {} {}: env2: {}", path, re, L, env2);
                env2.whenComplete((r, ex) -> logger.trace("env_L {} {} {}: result2 {}", path, re, L, env2));
                envs.add(env2);
            }
            final AggregateFuture<Env<S, L, D>> listEnv = new AggregateFuture<>(envs);
            logger.trace("env_L {} {} {}: listEnv: {}", path, re, L, listEnv);
            listEnv.whenComplete((r, ex) -> logger.trace("env_L {} {} {}: listResult {}", path, re, L, listEnv));
            final IFuture<Env<S, L, D>> env = listEnv.thenApply((es) -> {
                final Env.Builder<S, L, D> envBuilder = Env.builder();
                es.forEach(envBuilder::addAll);
                return envBuilder.build();
            });
            logger.trace("env_L {} {} {}: env: {}", path, re, L, env);
            env.whenComplete((r, ex) -> logger.trace("env_L {} {} {}: result {}", path, re, L, env));
            return env;
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
        logger.trace("env_data {} {}", path, re);
        try {
            if(!re.accepting()) {
                final IFuture<Env<S, L, D>> env = empty();
                logger.trace("env_data {} {}: empty {}", path, re, env);
                return env;
            }
            final IFuture<Void> guard = isComplete.apply(path.getTarget(), dataLabel);
            logger.trace("env_data {} {}: guard {}", path, re, guard);
            guard.whenComplete((r, ex) -> logger.trace("env_data {} {}: pass {}", path, re, guard));
            final IFuture<Env<S, L, D>> env = guard.thenApply(ignored -> {
                final D datum;
                if((datum = getData(re, path).orElse(null)) == null || !dataWF.wf(datum)) {
                    return Env.empty();
                }
                logger.trace("env_data {} {}: datum {}", path, re, datum);
                final IResolutionPath<S, L, D> resPath = Paths.resolve(path, datum);
                return Env.of(resPath);
            });
            logger.trace("env_data {} {}: env {}", path, re, env);
            env.whenComplete((r, ex) -> logger.trace("env_data {} {}: result {}", path, re, env));
            return env;
        } catch(ResolutionException | InterruptedException ex) {
            return CompletableFuture.completedExceptionally(ex);
        }
    }

    private IFuture<Env<S, L, D>> env_edges(IScopePath<S, L> path, LabelWF<L> re, L l, ICancel cancel) {
        logger.trace("env_edges {} {} {}", path, re, l);
        try {
            final LabelWF<L> newRe;
            if((newRe = re.step(l).orElse(null)) == null) {
                final IFuture<Env<S, L, D>> env = empty();
                logger.trace("env_edges {} {} {}: empty {}", path, re, l, env);
                return env;
            }
            final EdgeOrData<L> edgeLabel = EdgeOrData.edge(l);
            final IFuture<Void> guard = isComplete.apply(path.getTarget(), edgeLabel);
            logger.trace("env_edges {} {} {}: guard {}", path, re, l, guard);
            guard.whenComplete((r, ex) -> logger.trace("env_edges {} {} {}: pass {}", path, re, l, guard));
            return guard.thenCompose(ignored -> {
                List<IFuture<Env<S, L, D>>> envs = Lists.newArrayList();
                for(S nextScope : getEdges(newRe, path, l)) {
                    final Optional<IScopePath<S, L>> p = Paths.append(path, Paths.edge(path.getTarget(), l, nextScope));
                    if(p.isPresent()) {
                        envs.add(env(p.get(), newRe, cancel));
                    }
                }
                final AggregateFuture<Env<S, L, D>> listEnv = new AggregateFuture<>(envs);
                logger.trace("env_edges {} {} {}: listEnv {}", path, re, l, listEnv);
                listEnv.whenComplete(
                        (r, ex) -> logger.trace("env_edges {} {} {}: listResult {}", path, re, l, listEnv));
                final IFuture<Env<S, L, D>> env = listEnv.thenApply(es -> {
                    final Env.Builder<S, L, D> envBuilder = Env.builder();
                    es.forEach(envBuilder::addAll);
                    return envBuilder.build();
                });
                logger.trace("env_edges {} {} {}: env {}", path, re, l, env);
                env.whenComplete((r, ex) -> logger.trace("env_edges {} {} {}: result {}", path, re, l, env));
                return env;
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
        return CompletableFuture.completedFuture(Env.empty());
    }

    ///////////////////////////////////////////////////////////////////////////
    // edges and data                                                        //
    ///////////////////////////////////////////////////////////////////////////

    protected Optional<D> getData(LabelWF<L> re, IScopePath<S, L> path) {
        return scopeGraph.get().getData(path.getTarget());
    }

    protected Iterable<S> getEdges(LabelWF<L> re, IScopePath<S, L> path, L l) {
        return scopeGraph.get().getEdges(path.getTarget(), l);
    }

}