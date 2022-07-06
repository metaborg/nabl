package mb.scopegraph.ecoop21;

import java.util.List;
import java.util.Optional;

import org.metaborg.util.Ref;
import org.metaborg.util.collection.CapsuleUtil;
import org.metaborg.util.future.AggregateFuture;
import org.metaborg.util.future.CompletableFuture;
import org.metaborg.util.future.Futures;
import org.metaborg.util.future.IFuture;
import org.metaborg.util.log.ILogger;
import org.metaborg.util.log.LoggerUtils;
import org.metaborg.util.task.ICancel;
import org.metaborg.util.tuple.Tuple2;
import org.metaborg.util.unit.Unit;

import com.google.common.collect.Lists;

import io.usethesource.capsule.Set;
import mb.scopegraph.oopsla20.reference.EdgeOrData;
import mb.scopegraph.oopsla20.reference.Env;
import mb.scopegraph.oopsla20.terms.newPath.ResolutionPath;
import mb.scopegraph.oopsla20.terms.newPath.ScopePath;

public class NameResolution<S, L, D, M> {

    private static final ILogger logger = LoggerUtils.logger(NameResolution.class);

    private final EdgeOrData<L> dataLabel;
    private final Set.Immutable<L> edgeLabels;

    private final LabelOrder<L> labelOrder;

    private final INameResolutionContext<S, L, D, M> context;

    public NameResolution(Set.Immutable<L> edgeLabels, LabelOrder<L> labelOrder, INameResolutionContext<S, L, D, M> context) {
        this.dataLabel = EdgeOrData.data();
        this.edgeLabels = edgeLabels;

        this.labelOrder = labelOrder;

        this.context = context;
    }

    ///////////////////////////////////////////////////////////////////////////

    public IFuture<Tuple2<Env<S, L, D>, M>> env(ScopePath<S, L> path, LabelWf<L> re, ICancel cancel) {
        final Set.Transient<EdgeOrData<L>> labels = CapsuleUtil.transientSet();
        if(re.accepting()) {
            labels.__insert(dataLabel);
        }
        for(L l : edgeLabels) {
            if(re.step(l).isPresent()) {
                labels.__insert(EdgeOrData.edge(l));
            }
        }
        return env_L(path, re, labels.freeze(), cancel);
    }

    private IFuture<Tuple2<Env<S, L, D>, M>> env_L(ScopePath<S, L> path, LabelWf<L> re, Set.Immutable<EdgeOrData<L>> L,
            ICancel cancel) {
        logger.trace("env_L {} {} {}", path, re, L);
        if(cancel.cancelled()) {
            return CompletableFuture.completedExceptionally(new InterruptedException());
        }
        final Set<EdgeOrData<L>> max_L = max(L);
        final List<IFuture<Tuple2<Env<S, L, D>, M>>> envs = Lists.newArrayList();
        for(EdgeOrData<L> l : max_L) {
            envs.add(env_lL(path, re, l, smaller(L, l), cancel));
        }
        final IFuture<List<Tuple2<Env<S, L, D>, M>>> listEnv = AggregateFuture.of(envs);
        logger.trace("env_L {} {} {}: listEnv: {}", path, re, L, listEnv);
        listEnv.whenComplete((r, ex) -> logger.trace("env_L {} {} {}: listResult {}", path, re, L, listEnv));
        final IFuture<Tuple2<Env<S, L, D>, M>> env = listEnv.thenApply((es) -> {
            final Env.Builder<S, L, D> envBuilder = Env.builder();
            M metadata = context.unitMetadata();
            for(Tuple2<Env<S,L,D>, M> e : es) {
                envBuilder.addAll(e._1());
                metadata = context.compose(metadata, e._2());
            }
            return Tuple2.of(envBuilder.build(), metadata);
        });
        logger.trace("env_L {} {} {}: env: {}", path, re, L, env);
        env.whenComplete((r, ex) -> logger.trace("env_L {} {} {}: result {}", path, re, L, env));
        return env;
    }

    private IFuture<Tuple2<Env<S, L, D>, M>> env_lL(ScopePath<S, L> path, LabelWf<L> re, EdgeOrData<L> l,
            Set.Immutable<EdgeOrData<L>> L, ICancel cancel) {
        final IFuture<Tuple2<Env<S, L, D>, M>> env1 = env_L(path, re, L, cancel);
        logger.trace("env_lL {} {} {}: env1: {}", path, re, L, env1);
        env1.whenComplete((r, ex) -> logger.trace("env_lL {} {} {}: result1: {}", path, re, L, r));
        return env1.thenCompose(e1 -> {
            final IFuture<Boolean> envComplete =
                    e1._1().isEmpty() ? CompletableFuture.completedFuture(false) : context.dataLeqAlwaysTrue(cancel);
            return envComplete.thenCompose(complete -> {
                if(complete) {
                    logger.trace("env_lL {} {} {}: env2 fully shadowed", path, re, L);
                    return CompletableFuture.completedFuture(e1);
                }
                final IFuture<Tuple2<Env<S, L, D>, M>> env2 = env_l(path, re, l, cancel);
                logger.trace("env_lL {} {} {}: env2: {}", path, re, L, env2);
                env2.whenComplete((r, ex) -> logger.trace("env_lL {} {} {}: result2 {}", path, re, L, r));
                return env2.thenCompose(e2 -> {
                    return shadows(e1._1(), e2._1(), cancel).thenApply(result -> {
                        return Tuple2.of(result._1(), context.compose(result._2(), context.compose(e1._2(), e2._2())));
                    });
                });
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

    private IFuture<Tuple2<Env<S, L, D>, M>> env_l(ScopePath<S, L> path, LabelWf<L> re, EdgeOrData<L> l, ICancel cancel) {
        return l.match(() -> env_data(path, re, cancel), lbl -> env_edges(path, re, lbl, cancel));
    }

    private IFuture<Tuple2<Env<S, L, D>, M>> env_data(ScopePath<S, L> path, LabelWf<L> re, ICancel cancel) {
        logger.trace("env_data {} {}", path, re);
        final IFuture<Optional<D>> datum = context.getDatum(path.getTarget());
        logger.trace("env_data {} {}: datum {}", path, re, datum);
        final IFuture<Tuple2<Env<S, L, D>, M>> env = datum.thenCompose(_d -> {
            D d;
            if((d = _d.orElse(null)) == null) {
                return CompletableFuture.completedFuture(Tuple2.of(Env.empty(), context.unitMetadata()));
            }
            return context.dataWf(d, cancel).thenApply(wf -> {
                if(!wf._1()) {
                    return Tuple2.of(Env.empty(), context.unitMetadata());
                }
                logger.trace("env_data {} {}: datum {}", path, re, d);
                final ResolutionPath<S, L, D> resPath = path.resolve(d);
                return Tuple2.of(Env.of(resPath), wf._2());
            });
        });
        logger.trace("env_data {} {}: env {}", path, re, env);
        env.whenComplete((r, ex) -> logger.trace("env_data {} {}: result {}", path, re, env));
        return env;
    }

    private IFuture<Tuple2<Env<S, L, D>, M>> env_edges(ScopePath<S, L> path, LabelWf<L> re, L l, ICancel cancel) {
        logger.trace("env_edges {} {} {}", path, re, l);
        final LabelWf<L> newRe = re.step(l).get();
        final IFuture<Iterable<S>> scopes = context.getEdges(path.getTarget(), l);
        logger.trace("env_edges {} {} {}: edge scopes {}", path, re, l, scopes);
        return scopes.thenCompose(ss -> {
            List<IFuture<Tuple2<Env<S, L, D>, M>>> envs = Lists.newArrayList();
            for(S nextScope : ss) {
                final Optional<ScopePath<S, L>> p = path.step(l, nextScope);
                if(p.isPresent()) {
                    envs.add(context.externalEnv(p.get(), newRe, labelOrder, cancel));
                } else {
                    // cycle
                }
            }
            final IFuture<List<Tuple2<Env<S, L, D>, M>>> listEnv = AggregateFuture.of(envs);
            logger.trace("env_edges {} {} {}: listEnv {}", path, re, l, listEnv);
            listEnv.whenComplete((r, ex) -> logger.trace("env_edges {} {} {}: listResult {}", path, re, l, listEnv));
            final IFuture<Tuple2<Env<S, L, D>, M>> env = listEnv.thenApply(es -> {
                final Env.Builder<S, L, D> envBuilder = Env.builder();
                M metadata = context.unitMetadata();
                for(Tuple2<Env<S, L, D>, M> subEnv : es) {
                    envBuilder.addAll(subEnv._1());
                    metadata = context.compose(metadata, subEnv._2());
                }
                return Tuple2.of(envBuilder.build(), metadata);
            });
            logger.trace("env_edges {} {} {}: env {}", path, re, l, env);
            env.whenComplete((r, ex) -> logger.trace("env_edges {} {} {}: result {}", path, re, l, env));
            return env;
        });
    }

    ///////////////////////////////////////////////////////////////////////////
    // environments                                                          //
    ///////////////////////////////////////////////////////////////////////////

    private IFuture<Tuple2<Env<S, L, D>, M>> shadows(Env<S, L, D> env1, Env<S, L, D> env2, ICancel cancel) {
        final Env.Builder<S, L, D> env = Env.builder();
        final Ref<M> metadata = new Ref<M>(context.unitMetadata());
        env.addAll(env1);
        return Futures.reduce(Unit.unit, env2, (u, p2) -> {
        return Futures.noneMatch(env1, p1 -> context.dataLeq(p2.getDatum(), p1.getDatum(), cancel).thenApply(res -> {
                metadata.set(context.compose(metadata.get(), res._2()));
                return res._1();
            })).thenApply(noneMatch -> {
                if(noneMatch) {
                    env.add(p2);
                }
                return Unit.unit;
            });
        }).thenApply(u -> Tuple2.of(env.build(), metadata.get()));
    }

}
