package mb.statix.concurrent.p_raffrayi.impl;

import java.util.List;
import java.util.Optional;

import org.metaborg.util.log.ILogger;
import org.metaborg.util.log.LoggerUtils;
import org.metaborg.util.task.ICancel;
import org.metaborg.util.unit.Unit;

import com.google.common.collect.Lists;

import io.usethesource.capsule.Set;
import mb.nabl2.util.CapsuleUtil;
import mb.statix.concurrent.actors.futures.AggregateFuture;
import mb.statix.concurrent.actors.futures.CompletableFuture;
import mb.statix.concurrent.actors.futures.Futures;
import mb.statix.concurrent.actors.futures.ICompletableFuture;
import mb.statix.concurrent.actors.futures.IFuture;
import mb.statix.concurrent.p_raffrayi.nameresolution.LabelOrder;
import mb.statix.concurrent.p_raffrayi.nameresolution.LabelWF;
import mb.statix.scopegraph.path.IResolutionPath;
import mb.statix.scopegraph.path.IScopePath;
import mb.statix.scopegraph.reference.EdgeOrData;
import mb.statix.scopegraph.reference.Env;
import mb.statix.scopegraph.terms.path.Paths;

abstract class NameResolution<S, L, D> {

    private static final ILogger logger = LoggerUtils.logger(NameResolution.class);

    private final EdgeOrData<L> dataLabel;
    private final Set.Immutable<L> edgeLabels;

    private final LabelOrder<L> labelOrder;

    public NameResolution(Iterable<L> edgeLabels, LabelOrder<L> labelOrder) {
        this.dataLabel = EdgeOrData.data();
        this.edgeLabels = CapsuleUtil.toSet(edgeLabels);

        this.labelOrder = labelOrder;
    }

    ///////////////////////////////////////////////////////////////////////////

    protected abstract Optional<IFuture<Env<S, L, D>>> externalEnv(IScopePath<S, L> path, LabelWF<L> re,
            LabelOrder<L> labelOrder);

    protected abstract IFuture<Optional<D>> getDatum(S scope);

    protected abstract IFuture<Iterable<S>> getEdges(S scope, L label);

    protected abstract IFuture<Boolean> dataWf(D datum, ICancel cancel) throws InterruptedException;

    protected abstract IFuture<Boolean> dataLeq(D d1, D d2, ICancel cancel) throws InterruptedException;

    ///////////////////////////////////////////////////////////////////////////

    public ICompletableFuture<Env<S, L, D>> env(IScopePath<S, L> path, LabelWF<L> re, ICancel cancel) {
        final ICompletableFuture<Env<S, L, D>> result = new CompletableFuture<>();
        logger.trace("env {}", path);
        final Set.Transient<EdgeOrData<L>> labels = CapsuleUtil.transientSet();
        if(re.accepting()) {
            labels.__insert(dataLabel);
        }
        for(L l : edgeLabels) {
            if(re.step(l).isPresent()) {
                labels.__insert(EdgeOrData.edge(l));
            }
        }
        externalEnv(path, re, labelOrder).orElseGet(() -> env_L(path, re, labels.freeze(), cancel))
                .whenComplete(result::complete);
        return result;
    }

    private IFuture<Env<S, L, D>> env_L(IScopePath<S, L> path, LabelWF<L> re, Set.Immutable<EdgeOrData<L>> L,
            ICancel cancel) {
        logger.trace("env_L {} {} {}", path, re, L);
        if(cancel.cancelled()) {
            return CompletableFuture.completedExceptionally(new InterruptedException());
        }
        final Set<EdgeOrData<L>> max_L = max(L);
        final List<IFuture<Env<S, L, D>>> envs = Lists.newArrayList();
        for(EdgeOrData<L> l : max_L) {
            envs.add(env_lL(path, re, l, smaller(L, l), cancel));
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
    }

    private IFuture<Env<S, L, D>> env_lL(IScopePath<S, L> path, LabelWF<L> re, EdgeOrData<L> l,
            Set.Immutable<EdgeOrData<L>> L, ICancel cancel) {
        final IFuture<Env<S, L, D>> env1 = env_L(path, re, L, cancel);
        logger.trace("env_L {} {} {}: env1: {}", path, re, L, env1);
        env1.whenComplete((r, ex) -> logger.trace("env_L {} {} {}: result1: {}", path, re, L, env1));
        return env1.thenCompose(e1 -> {
            //if(!e1.isEmpty() && dataEquiv.alwaysTrue()) {
            //    return CompletableFuture.completedFuture(e1);
            //}
            final IFuture<Env<S, L, D>> env2 = env_l(path, re, l, cancel);
            logger.trace("env_L {} {} {}: env2: {}", path, re, L, env2);
            env2.whenComplete((r, ex) -> logger.trace("env_L {} {} {}: result2 {}", path, re, L, env2));
            return env2.thenCompose(e2 -> {
                return shadows(e1, e2, cancel);
            });
        });
    }

    private Set.Immutable<EdgeOrData<L>> max(Set.Immutable<EdgeOrData<L>> L) {
        final Set.Transient<EdgeOrData<L>> max = CapsuleUtil.transientSet();
        outer: for(EdgeOrData<L> l1 : L) {
            for(EdgeOrData<L> l2 : L) {
                try {
                    if(labelOrder.lt(l1, l2)) {
                        continue outer;
                    }
                } catch(Throwable t) {
                    logger.error("Unexpected exception in labelOrder", t);
                    continue outer;
                }
            }
            max.__insert(l1);
        }
        return max.freeze();
    }

    private Set.Immutable<EdgeOrData<L>> smaller(Set.Immutable<EdgeOrData<L>> L, EdgeOrData<L> l1) {
        final Set.Transient<EdgeOrData<L>> smaller = CapsuleUtil.transientSet();
        for(EdgeOrData<L> l2 : L) {
            if(labelOrder.lt(l2, l1)) {
                smaller.__insert(l2);
            }
        }
        return smaller.freeze();
    }

    private IFuture<Env<S, L, D>> env_l(IScopePath<S, L> path, LabelWF<L> re, EdgeOrData<L> l, ICancel cancel) {
        try {
            return l.matchInResolution(() -> env_data(path, re, cancel), lbl -> env_edges(path, re, lbl, cancel));
        } catch(Exception e) {
            throw new IllegalStateException("Should not happen.");
        }
    }

    private IFuture<Env<S, L, D>> env_data(IScopePath<S, L> path, LabelWF<L> re, ICancel cancel) {
        logger.trace("env_data {} {}", path, re);
        final IFuture<Optional<D>> datum = getDatum(path.getTarget());
        logger.trace("env_data {} {}: datum {}", path, re, datum);
        final IFuture<Env<S, L, D>> env = datum.thenCompose(d -> {
            if(!d.isPresent()) {
                return CompletableFuture.completedFuture(Env.empty());
            }
            return dataWf(d.get(), cancel).thenApply(wf -> {
                if(!wf) {
                    return Env.empty();
                }
                logger.trace("env_data {} {}: datum {}", path, re, d.get());
                final IResolutionPath<S, L, D> resPath = Paths.resolve(path, d.get());
                return Env.of(resPath);
            });
        });
        logger.trace("env_data {} {}: env {}", path, re, env);
        env.whenComplete((r, ex) -> logger.trace("env_data {} {}: result {}", path, re, env));
        return env;
    }

    private IFuture<Env<S, L, D>> env_edges(IScopePath<S, L> path, LabelWF<L> re, L l, ICancel cancel) {
        logger.trace("env_edges {} {} {}", path, re, l);
        final LabelWF<L> newRe = re.step(l).get();
        final IFuture<Iterable<S>> scopes = getEdges(path.getTarget(), l);
        logger.trace("env_edges {} {} {}: edge scopes {}", path, re, l, scopes);
        return scopes.thenCompose(ss -> {
            List<IFuture<Env<S, L, D>>> envs = Lists.newArrayList();
            for(S nextScope : ss) {
                final Optional<IScopePath<S, L>> p = Paths.append(path, Paths.edge(path.getTarget(), l, nextScope));
                if(p.isPresent()) {
                    envs.add(env(p.get(), newRe, cancel));
                }
            }
            final AggregateFuture<Env<S, L, D>> listEnv = new AggregateFuture<>(envs);
            logger.trace("env_edges {} {} {}: listEnv {}", path, re, l, listEnv);
            listEnv.whenComplete((r, ex) -> logger.trace("env_edges {} {} {}: listResult {}", path, re, l, listEnv));
            final IFuture<Env<S, L, D>> env = listEnv.thenApply(es -> {
                final Env.Builder<S, L, D> envBuilder = Env.builder();
                es.forEach(envBuilder::addAll);
                return envBuilder.build();
            });
            logger.trace("env_edges {} {} {}: env {}", path, re, l, env);
            env.whenComplete((r, ex) -> logger.trace("env_edges {} {} {}: result {}", path, re, l, env));
            return env;
        });
    }

    ///////////////////////////////////////////////////////////////////////////
    // environments                                                          //
    ///////////////////////////////////////////////////////////////////////////

    private IFuture<Env<S, L, D>> shadows(Env<S, L, D> env1, Env<S, L, D> env2, ICancel cancel) {
        final Env.Builder<S, L, D> env = Env.builder();
        env.addAll(env1);
        return Futures.reduce(Unit.unit, env2, (u, p2) -> {
            return Futures.noneMatch(env1, p1 -> dataLeq(p2.getDatum(), p1.getDatum(), cancel)).thenApply(noneMatch -> {
                if(noneMatch) {
                    env.add(p2);
                }
                return Unit.unit;
            });
        }).thenApply(u -> env.build());
    }

}
