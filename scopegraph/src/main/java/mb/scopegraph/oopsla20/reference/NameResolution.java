package mb.scopegraph.oopsla20.reference;

import java.util.Optional;

import org.metaborg.util.collection.CapsuleUtil;
import org.metaborg.util.functions.Predicate2;
import org.metaborg.util.task.ICancel;

import io.usethesource.capsule.Set;
import io.usethesource.capsule.util.stream.CapsuleCollectors;
import mb.scopegraph.oopsla20.INameResolution;
import mb.scopegraph.oopsla20.IScopeGraph;
import mb.scopegraph.oopsla20.terms.newPath.ResolutionPath;
import mb.scopegraph.oopsla20.terms.newPath.ScopePath;

public class NameResolution<S extends D, L, D> implements INameResolution<S, L, D> {

    private final IScopeGraph<S, L, D> scopeGraph;

    private final EdgeOrData<L> dataLabel;
    private final java.util.Set<EdgeOrData<L>> allLabels;

    private final LabelWF<L> labelWF; // default: true
    private final LabelOrder<L> labelOrder; // default: false

    private final DataWF<D> dataWF; // default: true
    private final DataLeq<D> dataEquiv; // default: true

    private final Predicate2<S, EdgeOrData<L>> isComplete; // default: true

    public NameResolution(IScopeGraph<S, L, D> scopeGraph, java.util.Set<L> edgeLabels, LabelWF<L> labelWF,
            LabelOrder<L> labelOrder, DataWF<D> dataWF, DataLeq<D> dataEquiv, Predicate2<S, EdgeOrData<L>> isComplete) {
        this.scopeGraph = scopeGraph;
        this.dataLabel = EdgeOrData.data();
        this.allLabels =
            edgeLabels.stream().map(EdgeOrData::edge).collect(CapsuleCollectors.toSet())
                .__insert(dataLabel);
        this.labelWF = labelWF;
        this.labelOrder = labelOrder;
        this.dataWF = dataWF;
        this.dataEquiv = dataEquiv;
        this.isComplete = isComplete;
    }

    @Override public Env<S, L, D> resolve(S scope, ICancel cancel) throws ResolutionException, InterruptedException {
        return env(labelWF, new ScopePath<>(scope), cancel);
    }

    private Env<S, L, D> env(LabelWF<L> re, ScopePath<S, L> path, ICancel cancel)
            throws ResolutionException, InterruptedException {
        return env_L(allLabels, re, path, cancel);
    }

    // FIXME Use caching of single label environments to prevent recalculation in case of diamonds in
    // the graph
    private Env<S, L, D> env_L(java.util.Set<EdgeOrData<L>> L, LabelWF<L> re, ScopePath<S, L> path, ICancel cancel)
            throws ResolutionException, InterruptedException {
        cancel.throwIfCancelled();
        final Env.Builder<S, L, D> env = Env.builder();
        final java.util.Set<EdgeOrData<L>> max_L = max(L);
        for(EdgeOrData<L> l : max_L) {
            final Env<S, L, D> env1 = env_L(smaller(L, l), re, path, cancel);
            env.addAll(env1);
            if(env1.isEmpty() || !dataEquiv.alwaysTrue()) {
                final Env<S, L, D> env2 = env_l(l, re, path, cancel);
                env.addAll(minus(env2, env1));
            }
        }
        return env.build();
    }

    private java.util.Set<EdgeOrData<L>> max(java.util.Set<EdgeOrData<L>> L) throws ResolutionException, InterruptedException {
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

    private java.util.Set<EdgeOrData<L>> smaller(java.util.Set<EdgeOrData<L>> L, EdgeOrData<L> l1)
            throws ResolutionException, InterruptedException {
        final Set.Transient<EdgeOrData<L>> smaller = CapsuleUtil.transientSet();
        for(EdgeOrData<L> l2 : L) {
            if(labelOrder.lt(l2, l1)) {
                smaller.__insert(l2);
            }
        }
        return smaller.freeze();
    }

    private Env<S, L, D> minus(Env<S, L, D> env1, Env<S, L, D> env2) throws ResolutionException, InterruptedException {
        final Env.Builder<S, L, D> env = Env.builder();
        outer: for(ResolutionPath<S, L, D> p1 : env1) {
            for(ResolutionPath<S, L, D> p2 : env2) {
                if(dataEquiv.leq(p2.getDatum(), p1.getDatum())) {
                    continue outer;
                }
            }
            env.add(p1);
        }
        return env.build();
    }

    private Env<S, L, D> env_l(EdgeOrData<L> l, LabelWF<L> re, ScopePath<S, L> path, ICancel cancel)
            throws ResolutionException, InterruptedException {
        return l.matchInResolution(() -> env_data(re, path), lbl -> env_edges(lbl, re, path, cancel));
    }

    private Env<S, L, D> env_data(LabelWF<L> re, ScopePath<S, L> path)
            throws ResolutionException, InterruptedException {
        if(!re.accepting()) {
            return Env.empty();
        }
        if(!isComplete.test(path.getTarget(), dataLabel)) {
            throw new IncompleteException(path.getTarget(), dataLabel);
        }
        final D datum;
        if((datum = getData(re, path).orElse(null)) == null || !dataWF.wf(datum)) {
            return Env.empty();
        }
        return Env.of(path.resolve(datum));
    }

    private Env<S, L, D> env_edges(L l, LabelWF<L> re, ScopePath<S, L> path, ICancel cancel)
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
                env.addAll(env(re, p.get(), cancel));
            }
        }
        return env.build();
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

    public static <S extends D, L, D> Builder<S, L, D> builder() {
        return new Builder<>();
    }

    public static class Builder<S extends D, L, D> implements INameResolution.Builder<S, L, D> {

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

        @Override public NameResolution<S, L, D> build(IScopeGraph<S, L, D> scopeGraph, java.util.Set<L> edgeLabels) {
            return new NameResolution<>(scopeGraph, edgeLabels, labelWF, labelOrder, dataWF, dataEquiv, isComplete);
        }

    }

}
