package mb.statix.scopegraph.reference;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.metaborg.util.functions.Predicate2;
import org.metaborg.util.task.ICancel;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Streams;

import mb.statix.scopegraph.INameResolution;
import mb.statix.scopegraph.IScopeGraph;
import mb.statix.scopegraph.path.IResolutionPath;
import mb.statix.scopegraph.path.IScopePath;
import mb.statix.scopegraph.terms.path.Paths;

public class NameResolution<S extends D, L, D> implements INameResolution<S, L, D> {

    private final IScopeGraph<S, L, D> scopeGraph;

    private final EdgeOrData<L> dataLabel;
    private final Set<EdgeOrData<L>> allLabels;

    private final LabelWF<L> labelWF; // default: true
    private final LabelOrder<L> labelOrder; // default: false

    private final DataWF<D> dataWF; // default: true
    private final DataLeq<D> dataEquiv; // default: false

    private final Predicate2<S, EdgeOrData<L>> isComplete; // default: true

    public NameResolution(IScopeGraph<S, L, D> scopeGraph, LabelWF<L> labelWF, LabelOrder<L> labelOrder,
            DataWF<D> dataWF, DataLeq<D> dataEquiv, Predicate2<S, EdgeOrData<L>> isComplete) {
        this.scopeGraph = scopeGraph;
        this.dataLabel = EdgeOrData.data(Access.INTERNAL);
        this.allLabels = Streams.concat(Stream.of(dataLabel), scopeGraph.getEdgeLabels().stream().map(EdgeOrData::edge))
                .collect(Collectors.toSet());
        this.labelWF = labelWF;
        this.labelOrder = labelOrder;
        this.dataWF = dataWF;
        this.dataEquiv = dataEquiv;
        this.isComplete = isComplete;
    }

    @Override public Env<S, L, D> resolve(S scope, ICancel cancel) throws ResolutionException, InterruptedException {
        return env(labelWF, Paths.empty(scope), cancel);
    }

    private Env<S, L, D> env(LabelWF<L> re, IScopePath<S, L> path, ICancel cancel)
            throws ResolutionException, InterruptedException {
        return env_L(allLabels, re, path, cancel);
    }

    // FIXME Use caching of single label environments to prevent recalculation in case of diamonds in
    // the graph
    private Env<S, L, D> env_L(Set<EdgeOrData<L>> L, LabelWF<L> re, IScopePath<S, L> path, ICancel cancel)
            throws ResolutionException, InterruptedException {
        cancel.throwIfCancelled();
        final Env.Builder<S, L, D> env = Env.builder();
        final Set<EdgeOrData<L>> max_L = max(L);
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

    private Env<S, L, D> env_l(EdgeOrData<L> l, LabelWF<L> re, IScopePath<S, L> path, ICancel cancel)
            throws ResolutionException, InterruptedException {
        return l.matchInResolution(acc -> env_data(re, path), lbl -> env_edges(lbl, re, path, cancel));
    }

    private Env<S, L, D> env_data(LabelWF<L> re, IScopePath<S, L> path)
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
        return Env.of(Paths.resolve(path, datum));
    }

    private Env<S, L, D> env_edges(L l, LabelWF<L> re, IScopePath<S, L> path, ICancel cancel)
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
            final Optional<IScopePath<S, L>> p = Paths.append(path, Paths.edge(path.getTarget(), l, nextScope));
            if(p.isPresent()) {
                env.addAll(env(re, p.get(), cancel));
            }
        }
        return env.build();
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

    public static <S extends D, L, D> Builder<S, L, D> builder() {
        return new Builder<>();
    }

    public static class Builder<S extends D, L, D> implements INameResolution.Builder<S, L, D> {

        private LabelWF<L> labelWF = LabelWF.ANY();
        private LabelOrder<L> labelOrder = LabelOrder.NONE();

        private DataWF<D> dataWF = DataWF.ANY();
        private DataLeq<D> dataEquiv = DataLeq.NONE();

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

        @Override public NameResolution<S, L, D> build(IScopeGraph<S, L, D> scopeGraph) {
            return new NameResolution<>(scopeGraph, labelWF, labelOrder, dataWF, dataEquiv, isComplete);
        }

    }

}
