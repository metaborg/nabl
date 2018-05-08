package mb.statix.scopegraph.reference;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.metaborg.util.functions.Predicate2;

import com.google.common.collect.ImmutableSet;

import mb.statix.scopegraph.INameResolution;
import mb.statix.scopegraph.IScopeGraph;
import mb.statix.scopegraph.path.IResolutionPath;
import mb.statix.scopegraph.path.IScopePath;
import mb.statix.scopegraph.terms.path.Paths;

public class NameResolution<S, L, R, O> implements INameResolution<S, L, R, O> {

    // relations R
    // order : V x V --- isTrue
    // isDataComplete : S x R

    private final IScopeGraph<S, L, R, O> scopeGraph;
    private final R relation;

    private final LabelWF<L> labelWF; // default: true
    private final LabelOrder<L> labelOrder; // default: false
    private final Predicate2<S, L> isEdgeComplete; // default: true

    private final DataWF<O> dataWF; // default: true
    private final DataEquiv<O> dataEquiv; // default: false
    private final Predicate2<S, R> isDataComplete; // default: true

    public NameResolution(IScopeGraph<S, L, R, O> scopeGraph, R relation, LabelWF<L> labelWF, LabelOrder<L> labelOrder,
            Predicate2<S, L> isEdgeComplete, DataWF<O> dataWF, DataEquiv<O> dataEquiv,
            Predicate2<S, R> isDataComplete) {
        super();
        this.scopeGraph = scopeGraph;
        this.relation = relation;
        this.labelWF = labelWF;
        this.labelOrder = labelOrder;
        this.isEdgeComplete = isEdgeComplete;
        this.dataWF = dataWF;
        this.dataEquiv = dataEquiv;
        this.isDataComplete = isDataComplete;
    }

    @Override public Set<IResolutionPath<S, L, R, O>> resolve(S scope) throws ResolutionException {
        return env(labelWF, Paths.empty(scope));
    }

    private Set<IResolutionPath<S, L, R, O>> env(LabelWF<L> re, IScopePath<S, L, O> path) throws ResolutionException {
        if(re.empty()) {
            return ImmutableSet.of();
        } else {
            return env_L(scopeGraph.getLabels(), re, path);
        }
    }

    // FIXME Use caching of single label environments to prevent recalculation in case of diamonds in
    //       the graph
    private Set<IResolutionPath<S, L, R, O>> env_L(Set<L> L, LabelWF<L> re, IScopePath<S, L, O> path)
            throws ResolutionException {
        final ImmutableSet.Builder<IResolutionPath<S, L, R, O>> envBuilder = ImmutableSet.builder();
        final Set<L> max_L = max(L);
        for(L l : max_L) {
            final Set<IResolutionPath<S, L, R, O>> env1 = env_L(smaller(L, l), re, path);
            envBuilder.addAll(env1);
            if(!dataEquiv.alwaysTrue() || env1.isEmpty()) {
                final Set<IResolutionPath<S, L, R, O>> env2 = env_l(l, re, path);
                envBuilder.addAll(minus(env2, env1));
            }
        }
        return envBuilder.build();
    }

    private Set<L> max(Set<L> L) {
        return L.stream().filter(l1 -> L.stream().noneMatch(l2 -> labelOrder.lt(l1, l2))).collect(Collectors.toSet());
    }

    private Set<L> smaller(Set<L> L, L l1) {
        return L.stream().filter(l2 -> labelOrder.lt(l2, l1)).collect(Collectors.toSet());
    }

    private Set<IResolutionPath<S, L, R, O>> minus(Set<IResolutionPath<S, L, R, O>> env1,
            Set<IResolutionPath<S, L, R, O>> env2) {
        return env1.stream().filter(p1 -> env2.stream().noneMatch(p2 -> {
            return dataEquiv.eq(p1.getDeclaration(), p2.getDeclaration());
        })).collect(Collectors.toSet());
    }

    private Set<IResolutionPath<S, L, R, O>> env_l(L l, LabelWF<L> re, IScopePath<S, L, O> path)
            throws ResolutionException {
        return l.equals(scopeGraph.getEndOfPath()) ? env_EOP(re, path) : env_nonEOP(l, re, path);
    }

    private Set<IResolutionPath<S, L, R, O>> env_EOP(LabelWF<L> re, IScopePath<S, L, O> path)
            throws ResolutionException {
        if(!isDataComplete.test(path.getTarget(), relation)) {
            throw new ResolutionException("Scope " + path.getTarget() + " is incomplete in data.");
        }
        if(!re.wf()) {
            return ImmutableSet.of();
        }
        ImmutableSet.Builder<IResolutionPath<S, L, R, O>> env = ImmutableSet.builder();
        for(O decl : scopeGraph.getData().get(path.getTarget(), relation)) {
            if(dataWF.wf(decl)) {
                env.add(Paths.resolve(path, relation, decl));
            }
        }
        return env.build();
    }

    private Set<IResolutionPath<S, L, R, O>> env_nonEOP(L l, LabelWF<L> re, IScopePath<S, L, O> path)
            throws ResolutionException {
        if(!isEdgeComplete.test(path.getTarget(), l)) {
            throw new ResolutionException("Scope " + path.getTarget() + " is incomplete in edge " + l);
        }
        final ImmutableSet.Builder<IResolutionPath<S, L, R, O>> env = ImmutableSet.builder();
        for(S nextScope : scopeGraph.getEdges().get(path.getTarget(), l)) {
            final Optional<IScopePath<S, L, O>> p = Paths.append(path, Paths.edge(path.getTarget(), l, nextScope));
            if(p.isPresent()) {
                env.addAll(env(re.step(l), p.get()));
            }
        }
        return env.build();
    }

    public static <S, L, R, O> Builder<S, L, R, O> builder() {
        return new Builder<>();
    }

    public static class Builder<S, L, R, O> {

        private LabelWF<L> labelWF = LabelWF.ANY();
        private LabelOrder<L> labelOrder = LabelOrder.NONE();
        private Predicate2<S, L> isEdgeComplete = (s, l) -> true;

        private DataWF<O> dataWF = DataWF.ANY();
        private DataEquiv<O> dataEquiv = DataEquiv.NONE();
        private Predicate2<S, R> isDataComplete = (s, r) -> true;

        public Builder<S, L, R, O> withLabelWF(LabelWF<L> labelWF) {
            this.labelWF = labelWF;
            return this;
        }

        public Builder<S, L, R, O> withLabelOrder(LabelOrder<L> labelOrder) {
            this.labelOrder = labelOrder;
            return this;
        }

        public Builder<S, L, R, O> withDataWF(DataWF<O> dataWF) {
            this.dataWF = dataWF;
            return this;
        }

        public Builder<S, L, R, O> withDataEquiv(DataEquiv<O> dataEquiv) {
            this.dataEquiv = dataEquiv;
            return this;
        }

        public NameResolution<S, L, R, O> build(IScopeGraph<S, L, R, O> scopeGraph, R relation) {
            return new NameResolution<>(scopeGraph, relation, labelWF, labelOrder, isEdgeComplete, dataWF, dataEquiv,
                    isDataComplete);
        }

    }

}