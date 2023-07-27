package mb.scopegraph.oopsla20.reference;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.metaborg.util.collection.CapsuleUtil;
import org.metaborg.util.functions.Predicate2;
import org.metaborg.util.iterators.Iterables2;
import org.metaborg.util.task.ICancel;
import org.metaborg.util.tuple.Tuple2;

import io.usethesource.capsule.Set;
import io.usethesource.capsule.util.stream.CapsuleCollectors;
import mb.scopegraph.oopsla20.INameResolution;
import mb.scopegraph.oopsla20.IScopeGraph;
import mb.scopegraph.oopsla20.terms.newPath.ResolutionPath;
import mb.scopegraph.oopsla20.terms.newPath.ScopePath;

public class FastNameResolution<S, L, D> implements INameResolution<S, L, D> {

    private final IScopeGraph<S, L, D> scopeGraph;

    private final EdgeOrData<L> dataLabel;
    private final Set.Immutable<EdgeOrData<L>> allLabels;

    private final LabelWF<L> labelWF; // default: true
    private final LabelOrder<L> labelOrder; // default: false

    private final DataWF<D> dataWF; // default: true
    private final DataLeq<D> dataEquiv; // default: true

    private final Predicate2<S, EdgeOrData<L>> isComplete; // default: true

    public FastNameResolution(IScopeGraph<S, L, D> scopeGraph, java.util.Set<L> edgeLabels, LabelWF<L> labelWF,
            LabelOrder<L> labelOrder, DataWF<D> dataWF, DataLeq<D> dataEquiv, Predicate2<S, EdgeOrData<L>> isComplete) {
        this.scopeGraph = scopeGraph;
        this.dataLabel = EdgeOrData.data();
        this.allLabels =
                edgeLabels.stream().map(EdgeOrData::edge).collect(CapsuleCollectors.toSet()).__insert(dataLabel);
        this.labelWF = labelWF;
        this.labelOrder = labelOrder;
        this.dataWF = dataWF;
        this.dataEquiv = dataEquiv;
        this.isComplete = isComplete;
    }

    @Override public Env<S, L, D> resolve(S scope, ICancel cancel) throws ResolutionException, InterruptedException {
        return env(labelWF, new ScopePath<>(scope), Env.empty(), cancel);
    }

    private Env<S, L, D> env(LabelWF<L> re, ScopePath<S, L> path, Iterable<ResolutionPath<S, L, D>> specifics,
            ICancel cancel) throws ResolutionException, InterruptedException {
        return env_L(allLabels, re, path, specifics, cancel);
    }

    private Env<S, L, D> env_L(Set.Immutable<EdgeOrData<L>> L, LabelWF<L> re, ScopePath<S, L> path,
            Iterable<ResolutionPath<S, L, D>> specifics, ICancel cancel)
            throws ResolutionException, InterruptedException {
        cancel.throwIfCancelled();
        final Env.Builder<S, L, D> env = Env.builder();
        final Set.Immutable<EdgeOrData<L>> max_L = max(L);
        for(EdgeOrData<L> l : max_L) {
            final Set.Immutable<EdgeOrData<L>> smaller = smaller(L, l);
            final Env<S, L, D> env1 = env_L(smaller, re, path, specifics, cancel);
            env.addAll(env1);
            if(env1.isEmpty() || !dataEquiv.alwaysTrue()) {
                final Env<S, L, D> env2 = env_l(l, re, path, Iterables2.fromConcat(specifics, env1), cancel);
                env.addAll(env2);
            }
        }
        return env.build();
    }

    private Env<S, L, D> env_l(EdgeOrData<L> l, LabelWF<L> re, ScopePath<S, L> path,
            Iterable<ResolutionPath<S, L, D>> specifics, ICancel cancel)
            throws ResolutionException, InterruptedException {
        return l.matchInResolution(() -> env_data(re, path, specifics),
                lbl -> env_edges(lbl, re, path, specifics, cancel));
    }

    private Env<S, L, D> env_data(LabelWF<L> re, ScopePath<S, L> path, Iterable<ResolutionPath<S, L, D>> specifics)
            throws ResolutionException, InterruptedException {
        if(!re.accepting()) {
            return Env.empty();
        }
        if(!isComplete.test(path.getTarget(), dataLabel)) {
            throw new IncompleteException(path.getTarget(), dataLabel);
        }
        final D datum;
        if((datum = getData(re, path).orElse(null)) == null || !dataWF.wf(datum) || isShadowed(datum, specifics)) {
            return Env.empty();
        }
        return Env.of(path.resolve(datum));
    }

    private Env<S, L, D> env_edges(L l, LabelWF<L> re, ScopePath<S, L> path,
            Iterable<ResolutionPath<S, L, D>> specifics, ICancel cancel)
            throws ResolutionException, InterruptedException {
        final Optional<LabelWF<L>> newRe = re.step(l);
        if(!newRe.isPresent()) {
            return Env.empty();
        } else {
            re = newRe.get();
        }
        final EdgeOrData<L> edgeLabel = EdgeOrData.edge(l);
        if(!isComplete.test(path.getTarget(), edgeLabel)) {
            throw new IncompleteException(path.getTarget(), edgeLabel);
        }
        final Env.Builder<S, L, D> env = Env.builder();
        for(S nextScope : getEdges(re, path, l)) {
            final Optional<ScopePath<S, L>> p = path.step(l, nextScope);
            if(p.isPresent()) {
                env.addAll(env(re, p.get(), specifics, cancel));
            }
        }
        return env.build();
    }

    private boolean isShadowed(D datum, Iterable<ResolutionPath<S, L, D>> specifics)
            throws ResolutionException, InterruptedException {
        for(ResolutionPath<S, L, D> p : specifics) {
            if(dataEquiv.leq(p.getDatum(), datum)) {
                return true;
            }
        }
        return false;
    }

    ///////////////////////////////////////////////////////////////////////////
    // edges and data                                                        //
    ///////////////////////////////////////////////////////////////////////////

    protected Optional<D> getData(@SuppressWarnings("unused") LabelWF<L> re, ScopePath<S, L> path) {
        return scopeGraph.getData(path.getTarget());
    }

    protected Iterable<S> getEdges(@SuppressWarnings("unused") LabelWF<L> re, ScopePath<S, L> path, L l) {
        return scopeGraph.getEdges(path.getTarget(), l);
    }

    ///////////////////////////////////////////////////////////////////////////
    // max labels                                                            //
    ///////////////////////////////////////////////////////////////////////////

    private final Map<Set.Immutable<EdgeOrData<L>>, Set.Immutable<EdgeOrData<L>>> maxCache =
        new HashMap<>();

    private Set.Immutable<EdgeOrData<L>> max(Set.Immutable<EdgeOrData<L>> L)
            throws ResolutionException, InterruptedException {
        Set.Immutable<EdgeOrData<L>> max;
        if((max = maxCache.get(L)) == null) {
            maxCache.put(L, (max = computeMax(L)));
        }
        return max;
    }

    private Set.Immutable<EdgeOrData<L>> computeMax(Set.Immutable<EdgeOrData<L>> L)
            throws ResolutionException, InterruptedException {
        final Set.Transient<EdgeOrData<L>> max = CapsuleUtil.transientSet();
        outer: for(EdgeOrData<L> l1 : L) {
            for(EdgeOrData<L> l2 : L) {
                if(labelOrder.lt(l1, l2)) {
                    continue outer;
                }
            }
            max.__insert(l1);
        }
        return max.freeze();
    }

    ///////////////////////////////////////////////////////////////////////////
    // smaller labels                                                        //
    ///////////////////////////////////////////////////////////////////////////

    private final Map<Tuple2<Set.Immutable<EdgeOrData<L>>, EdgeOrData<L>>, Set.Immutable<EdgeOrData<L>>> smallerCache =
        new HashMap<>();

    private Set.Immutable<EdgeOrData<L>> smaller(Set.Immutable<EdgeOrData<L>> L, EdgeOrData<L> l1)
            throws ResolutionException, InterruptedException {
        Tuple2<Set.Immutable<EdgeOrData<L>>, EdgeOrData<L>> key = Tuple2.of(L, l1);
        Set.Immutable<EdgeOrData<L>> smaller;
        if(null == (smaller = smallerCache.get(key))) {
            smallerCache.put(key, (smaller = computeSmaller(L, l1)));
        }
        return smaller;
    }

    private Set.Immutable<EdgeOrData<L>> computeSmaller(Set.Immutable<EdgeOrData<L>> L, EdgeOrData<L> l1)
            throws ResolutionException, InterruptedException {
        final Set.Transient<EdgeOrData<L>> smaller = CapsuleUtil.transientSet();
        for(EdgeOrData<L> l2 : L) {
            if(labelOrder.lt(l2, l1)) {
                smaller.__insert(l2);
            }
        }
        return smaller.freeze();
    }

    ///////////////////////////////////////////////////////////////////////////
    // builder                                                               //
    ///////////////////////////////////////////////////////////////////////////

    public static <S, L, D> Builder<S, L, D> builder() {
        return new Builder<>();
    }

    public static class Builder<S, L, D> implements INameResolution.Builder<S, L, D> {

        private LabelWF<L> labelWF = LabelWF.ANY();
        private LabelOrder<L> labelOrder = LabelOrder.NONE();

        private DataWF<D> dataWF = DataWF.ANY();
        private DataLeq<D> dataEquiv = DataLeq.ALL();

        private Predicate2<S, EdgeOrData<L>> isComplete = (s, l) -> true;

        @Override public Builder<S, L, D> withLabelWF(LabelWF<L> labelWF) {
            this.labelWF = labelWF;
            return this;
        }

        @Override public Builder<S, L, D> withLabelOrder(LabelOrder<L> labelOrder) {
            this.labelOrder = labelOrder;
            return this;
        }

        @Override public Builder<S, L, D> withDataWF(DataWF<D> dataWF) {
            this.dataWF = dataWF;
            return this;
        }

        @Override public Builder<S, L, D> withDataEquiv(DataLeq<D> dataEquiv) {
            this.dataEquiv = dataEquiv;
            return this;
        }

        @Override public Builder<S, L, D> withIsComplete(Predicate2<S, EdgeOrData<L>> isComplete) {
            this.isComplete = isComplete;
            return this;
        }

        @Override public FastNameResolution<S, L, D> build(IScopeGraph<S, L, D> scopeGraph,
                java.util.Set<L> edgeLabels) {
            return new FastNameResolution<>(scopeGraph, edgeLabels, labelWF, labelOrder, dataWF, dataEquiv, isComplete);
        }

    }

}
