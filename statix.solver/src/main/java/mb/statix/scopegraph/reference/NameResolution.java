package mb.statix.scopegraph.reference;

import java.util.Optional;
import java.util.Set;

import org.metaborg.util.functions.Predicate2;

import com.google.common.collect.ImmutableSet;

import mb.statix.scopegraph.INameResolution;
import mb.statix.scopegraph.IScopeGraph;
import mb.statix.scopegraph.path.IResolutionPath;
import mb.statix.scopegraph.path.IScopePath;
import mb.statix.scopegraph.terms.path.Paths;

public class NameResolution<S extends D, L, D> implements INameResolution<S, L, D> {

    private final IScopeGraph<S, L, D> scopeGraph;
    private final Set<L> labels;

    private final LabelWF<L> labelWF; // default: true
    private final LabelOrder<L> labelOrder; // default: false
    private final Predicate2<S, L> isEdgeComplete; // default: true

    private final DataWF<D> dataWF; // default: true
    private final DataLeq<D> dataEquiv; // default: false
    private final Predicate2<S, L> isDataComplete; // default: true

    public NameResolution(IScopeGraph<S, L, D> scopeGraph, LabelWF<L> labelWF, LabelOrder<L> labelOrder,
            Predicate2<S, L> isEdgeComplete, DataWF<D> dataWF, DataLeq<D> dataEquiv, Predicate2<S, L> isDataComplete) {
        super();
        this.scopeGraph = scopeGraph;
        this.labels = ImmutableSet.<L>builder().addAll(scopeGraph.getEdgeLabels()).addAll(scopeGraph.getDataLabels())
                .add(scopeGraph.getNoDataLabel()).build();
        this.labelWF = labelWF;
        this.labelOrder = labelOrder;
        this.isEdgeComplete = isEdgeComplete;
        this.dataWF = dataWF;
        this.dataEquiv = dataEquiv;
        this.isDataComplete = isDataComplete;
    }

    @Override public Set<IResolutionPath<S, L, D>> resolve(S scope) throws ResolutionException, InterruptedException {
        return env(labelWF, Paths.empty(scope));
    }

    private Set<IResolutionPath<S, L, D>> env(LabelWF<L> re, IScopePath<S, L> path)
            throws ResolutionException, InterruptedException {
        return env_L(labels, re, path);
    }

    // FIXME Use caching of single label environments to prevent recalculation in case of diamonds in
    // the graph
    private Set<IResolutionPath<S, L, D>> env_L(Set<L> L, LabelWF<L> re, IScopePath<S, L> path)
            throws ResolutionException, InterruptedException {
        if(Thread.interrupted()) {
            throw new InterruptedException();
        }
        final ImmutableSet.Builder<IResolutionPath<S, L, D>> envBuilder = ImmutableSet.builder();
        final Set<L> max_L = max(L);
        for(L l : max_L) {
            final Set<IResolutionPath<S, L, D>> env1 = env_L(smaller(L, l), re, path);
            envBuilder.addAll(env1);
            if(!dataEquiv.alwaysTrue() || env1.isEmpty()) {
                final Set<IResolutionPath<S, L, D>> env2 = env_l(l, re, path);
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

    private Set<IResolutionPath<S, L, D>> minus(Set<IResolutionPath<S, L, D>> env1, Set<IResolutionPath<S, L, D>> env2)
            throws ResolutionException, InterruptedException {
        final ImmutableSet.Builder<IResolutionPath<S, L, D>> env = ImmutableSet.builder();
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

    private Set<IResolutionPath<S, L, D>> env_l(L l, LabelWF<L> re, IScopePath<S, L> path)
            throws ResolutionException, InterruptedException {
        final Optional<LabelWF<L>> newRe = re.step(l);
        if(!newRe.isPresent()) {
            return ImmutableSet.of();
        } else {
            re = newRe.get();
        }
        if(scopeGraph.getEdgeLabels().contains(l)) {
            return env_nonEOP(l, re, path);
        } else if(scopeGraph.getDataLabels().contains(l) || scopeGraph.getNoDataLabel().equals(l)) {
            return env_EOP(l, re, path);
        } else {
            throw new IllegalStateException("Encountered unknown label " + l);
        }
    }

    private Set<IResolutionPath<S, L, D>> env_EOP(L l, LabelWF<L> re, IScopePath<S, L> path)
            throws ResolutionException, InterruptedException {
        if(!re.accepting()) {
            return ImmutableSet.of();
        }
        final S scope = path.getTarget();
        if(!isDataComplete.test(scope, l)) {
            throw new IncompleteDataException(scope, l);
        }
        final ImmutableSet.Builder<IResolutionPath<S, L, D>> env = ImmutableSet.builder();
        if(l.equals(scopeGraph.getNoDataLabel())) {
            final D datum = scope;
            if(dataWF.wf(datum)) {
                env.add(Paths.resolve(path, l, datum));
            }
        } else {
            for(D datum : scopeGraph.getData().get(path.getTarget(), l)) {
                if(dataWF.wf(datum)) {
                    env.add(Paths.resolve(path, l, datum));
                }
            }
        }
        return env.build();
    }

    private Set<IResolutionPath<S, L, D>> env_nonEOP(L l, LabelWF<L> re, IScopePath<S, L> path)
            throws ResolutionException, InterruptedException {
        if(!isEdgeComplete.test(path.getTarget(), l)) {
            throw new IncompleteEdgeException(path.getTarget(), l);
        }
        final ImmutableSet.Builder<IResolutionPath<S, L, D>> env = ImmutableSet.builder();
        for(S nextScope : scopeGraph.getEdges().get(path.getTarget(), l)) {
            final Optional<IScopePath<S, L>> p = Paths.append(path, Paths.edge(path.getTarget(), l, nextScope));
            if(p.isPresent()) {
                env.addAll(env(re, p.get()));
            }
        }
        return env.build();
    }

    public static <S extends D, L, D> Builder<S, L, D> builder() {
        return new Builder<>();
    }

    public static class Builder<S extends D, L, D> {

        private LabelWF<L> labelWF = LabelWF.ANY();
        private LabelOrder<L> labelOrder = LabelOrder.NONE();
        private Predicate2<S, L> isEdgeComplete = (s, l) -> true;

        private DataWF<D> dataWF = DataWF.ANY();
        private DataLeq<D> dataEquiv = DataLeq.NONE();
        private Predicate2<S, L> isDataComplete = (s, r) -> true;

        public Builder<S, L, D> withLabelWF(LabelWF<L> labelWF) {
            this.labelWF = labelWF;
            return this;
        }

        public Builder<S, L, D> withLabelOrder(LabelOrder<L> labelOrder) {
            this.labelOrder = labelOrder;
            return this;
        }

        public Builder<S, L, D> withEdgeComplete(Predicate2<S, L> isEdgeComplete) {
            this.isEdgeComplete = isEdgeComplete;
            return this;
        }

        public Builder<S, L, D> withDataWF(DataWF<D> dataWF) {
            this.dataWF = dataWF;
            return this;
        }

        public Builder<S, L, D> withDataEquiv(DataLeq<D> dataEquiv) {
            this.dataEquiv = dataEquiv;
            return this;
        }

        public Builder<S, L, D> withDataComplete(Predicate2<S, L> isDataComplete) {
            this.isDataComplete = isDataComplete;
            return this;
        }

        public NameResolution<S, L, D> build(IScopeGraph<S, L, D> scopeGraph) {
            return new NameResolution<>(scopeGraph, labelWF, labelOrder, isEdgeComplete, dataWF, dataEquiv,
                    isDataComplete);
        }

    }

}