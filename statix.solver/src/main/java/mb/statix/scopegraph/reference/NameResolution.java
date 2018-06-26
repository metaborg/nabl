package mb.statix.scopegraph.reference;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.metaborg.util.functions.Predicate2;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

import mb.statix.scopegraph.INameResolution;
import mb.statix.scopegraph.IScopeGraph;
import mb.statix.scopegraph.path.IResolutionPath;
import mb.statix.scopegraph.path.IScopePath;
import mb.statix.scopegraph.terms.path.Paths;

public class NameResolution<V, L, R> implements INameResolution<V, L, R> {

    private final IScopeGraph<V, L, R> scopeGraph;
    private final Set<L> labels;
    private final Optional<R> relation;

    private final LabelWF<L> labelWF; // default: true
    private final LabelOrder<L> labelOrder; // default: false
    private final Predicate2<V, L> isEdgeComplete; // default: true

    private final DataWF<V> dataWF; // default: true
    private final DataEquiv<V> dataEquiv; // default: false
    private final Predicate2<V, R> isDataComplete; // default: true

    public NameResolution(IScopeGraph<V, L, R> scopeGraph, Optional<R> relation, LabelWF<L> labelWF,
            LabelOrder<L> labelOrder, Predicate2<V, L> isEdgeComplete, DataWF<V> dataWF, DataEquiv<V> dataEquiv,
            Predicate2<V, R> isDataComplete) {
        super();
        this.scopeGraph = scopeGraph;
        this.labels = ImmutableSet.<L>builder().addAll(scopeGraph.getLabels()).add(scopeGraph.getEndOfPath()).build();
        this.relation = relation;
        this.labelWF = labelWF;
        this.labelOrder = labelOrder;
        this.isEdgeComplete = isEdgeComplete;
        this.dataWF = dataWF;
        this.dataEquiv = dataEquiv;
        this.isDataComplete = isDataComplete;
    }

    @Override public Set<IResolutionPath<V, L, R>> resolve(V scope) throws ResolutionException, InterruptedException {
        return env(labelWF, Paths.empty(scope));
    }

    private Set<IResolutionPath<V, L, R>> env(LabelWF<L> re, IScopePath<V, L> path)
            throws ResolutionException, InterruptedException {
        return env_L(labels, re, path);
    }

    // FIXME Use caching of single label environments to prevent recalculation in case of diamonds in
    // the graph
    private Set<IResolutionPath<V, L, R>> env_L(Set<L> L, LabelWF<L> re, IScopePath<V, L> path)
            throws ResolutionException, InterruptedException {
        final ImmutableSet.Builder<IResolutionPath<V, L, R>> envBuilder = ImmutableSet.builder();
        final Set<L> max_L = max(L);
        for(L l : max_L) {
            final Set<IResolutionPath<V, L, R>> env1 = env_L(smaller(L, l), re, path);
            envBuilder.addAll(env1);
            if(!dataEquiv.alwaysTrue() || env1.isEmpty()) {
                final Set<IResolutionPath<V, L, R>> env2 = env_l(l, re, path);
                envBuilder.addAll(minus(env2, env1));
            }
        }
        return envBuilder.build();
    }

    private Set<L> max(Set<L> L) throws ResolutionException, InterruptedException {
        final ImmutableSet.Builder<L> max = ImmutableSet.builder();
        outer: for(L l1 : L) {
            for(L l2 : L) {
                if(labelOrder.lt(l1, l2)) {
                    continue outer;
                }
            }
            max.add(l1);
        }
        return max.build();
    }

    private Set<L> smaller(Set<L> L, L l1) throws ResolutionException, InterruptedException {
        final ImmutableSet.Builder<L> smaller = ImmutableSet.builder();
        for(L l2 : L) {
            if(labelOrder.lt(l2, l1)) {
                smaller.add(l2);
            }
        }
        return smaller.build();
    }

    private Set<IResolutionPath<V, L, R>> minus(Set<IResolutionPath<V, L, R>> env1, Set<IResolutionPath<V, L, R>> env2)
            throws ResolutionException, InterruptedException {
        final ImmutableSet.Builder<IResolutionPath<V, L, R>> env = ImmutableSet.builder();
        outer: for(IResolutionPath<V, L, R> p1 : env1) {
            for(IResolutionPath<V, L, R> p2 : env2) {
                if(dataEquiv.eq(p1.getDatum(), p2.getDatum())) {
                    continue outer;
                }
            }
            env.add(p1);
        }
        return env.build();
    }

    private Set<IResolutionPath<V, L, R>> env_l(L l, LabelWF<L> re, IScopePath<V, L> path)
            throws ResolutionException, InterruptedException {
        return l.equals(scopeGraph.getEndOfPath()) ? env_EOP(re, path) : env_nonEOP(l, re, path);
    }

    private Set<IResolutionPath<V, L, R>> env_EOP(LabelWF<L> re, IScopePath<V, L> path)
            throws ResolutionException, InterruptedException {
        if(!re.wf()) {
            return ImmutableSet.of();
        }
        final V scope = path.getTarget();
        if(relation.map(r -> !isDataComplete.test(scope, r)).orElse(false)) {
            throw new ResolutionException("Scope " + scope + " is incomplete in " + relation);
        }
        final ImmutableSet.Builder<IResolutionPath<V, L, R>> env = ImmutableSet.builder();
        if(relation.isPresent()) {
            for(List<V> datum : scopeGraph.getData().get(path.getTarget(), relation.get())) {
                if(dataWF.wf(datum)) {
                    env.add(Paths.resolve(path, relation, datum));
                }
            }
        } else {
            final List<V> datum = ImmutableList.of(scope);
            if(dataWF.wf(datum)) {
                env.add(Paths.resolve(path, relation, datum));
            }
        }
        return env.build();
    }

    private Set<IResolutionPath<V, L, R>> env_nonEOP(L l, LabelWF<L> re, IScopePath<V, L> path)
            throws ResolutionException, InterruptedException {
        final LabelWF<L> newRe = re.step(l);
        if(newRe.empty()) {
            return ImmutableSet.of();
        }
        if(!isEdgeComplete.test(path.getTarget(), l)) {
            throw new ResolutionException("Scope " + path.getTarget() + " is incomplete in edge " + l);
        }
        final ImmutableSet.Builder<IResolutionPath<V, L, R>> env = ImmutableSet.builder();
        for(V nextScope : scopeGraph.getEdges().get(path.getTarget(), l)) {
            final Optional<IScopePath<V, L>> p = Paths.append(path, Paths.edge(path.getTarget(), l, nextScope));
            if(p.isPresent()) {
                env.addAll(env(newRe, p.get()));
            }
        }
        return env.build();
    }

    public static <V, L, R> Builder<V, L, R> builder() {
        return new Builder<>();
    }

    public static class Builder<V, L, R> {

        private LabelWF<L> labelWF = LabelWF.ANY();
        private LabelOrder<L> labelOrder = LabelOrder.NONE();
        private Predicate2<V, L> isEdgeComplete = (s, l) -> true;

        private DataWF<V> dataWF = DataWF.ANY();
        private DataEquiv<V> dataEquiv = DataEquiv.NONE();
        private Predicate2<V, R> isDataComplete = (s, r) -> true;

        public Builder<V, L, R> withLabelWF(LabelWF<L> labelWF) {
            this.labelWF = labelWF;
            return this;
        }

        public Builder<V, L, R> withLabelOrder(LabelOrder<L> labelOrder) {
            this.labelOrder = labelOrder;
            return this;
        }

        public Builder<V, L, R> withEdgeComplete(Predicate2<V, L> isEdgeComplete) {
            this.isEdgeComplete = isEdgeComplete;
            return this;
        }

        public Builder<V, L, R> withDataWF(DataWF<V> dataWF) {
            this.dataWF = dataWF;
            return this;
        }

        public Builder<V, L, R> withDataEquiv(DataEquiv<V> dataEquiv) {
            this.dataEquiv = dataEquiv;
            return this;
        }

        public Builder<V, L, R> withDataComplete(Predicate2<V, R> isDataComplete) {
            this.isDataComplete = isDataComplete;
            return this;
        }

        public NameResolution<V, L, R> build(IScopeGraph<V, L, R> scopeGraph, Optional<R> relation) {
            return new NameResolution<>(scopeGraph, relation, labelWF, labelOrder, isEdgeComplete, dataWF, dataEquiv,
                    isDataComplete);
        }

    }

}