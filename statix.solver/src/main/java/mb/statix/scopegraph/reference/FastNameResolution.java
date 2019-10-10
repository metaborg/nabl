package mb.statix.scopegraph.reference;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.metaborg.util.functions.Predicate2;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;

import io.usethesource.capsule.Set;
import mb.nabl2.util.ImmutableTuple2;
import mb.nabl2.util.Tuple2;
import mb.statix.scopegraph.INameResolution;
import mb.statix.scopegraph.IScopeGraph;
import mb.statix.scopegraph.path.IResolutionPath;
import mb.statix.scopegraph.path.IScopePath;
import mb.statix.scopegraph.terms.path.Paths;

public class FastNameResolution<S extends D, L, D> implements INameResolution<S, L, D> {

    private final IScopeGraph<S, L, D> scopeGraph;
    private final L relation;
    private final Set.Immutable<L> labels;

    private final LabelWF<L> labelWF; // default: true
    private final LabelOrder<L> labelOrder; // default: false
    private final Predicate2<S, L> isEdgeComplete; // default: true

    private final DataWF<D> dataWF; // default: true
    private final DataLeq<D> dataEquiv; // default: false
    private final Predicate2<S, L> isDataComplete; // default: true

    public FastNameResolution(IScopeGraph<S, L, D> scopeGraph, L relation, LabelWF<L> labelWF, LabelOrder<L> labelOrder,
            Predicate2<S, L> isEdgeComplete, DataWF<D> dataWF, DataLeq<D> dataEquiv, Predicate2<S, L> isDataComplete) {
        this.scopeGraph = scopeGraph;
        this.relation = relation;
        this.labels =
                Set.Immutable.<L>of().__insertAll(scopeGraph.getEdgeLabels()).__insert(scopeGraph.getNoDataLabel());
        this.labelWF = labelWF;
        this.labelOrder = labelOrder;
        this.isEdgeComplete = isEdgeComplete;
        this.dataWF = dataWF;
        this.dataEquiv = dataEquiv;
        this.isDataComplete = isDataComplete;
    }

    @Override public List<IResolutionPath<S, L, D>> resolve(S scope) throws ResolutionException, InterruptedException {
        return env(labelWF, Paths.empty(scope), ImmutableList.of());
    }

    private List<IResolutionPath<S, L, D>> env(LabelWF<L> re, IScopePath<S, L> path,
            Iterable<IResolutionPath<S, L, D>> specifics) throws ResolutionException, InterruptedException {
        return env_L(labels, re, path, specifics);
    }

    // FIXME Use caching of single label environments to prevent recalculation in case of diamonds in
    // the graph
    private List<IResolutionPath<S, L, D>> env_L(Set.Immutable<L> L, LabelWF<L> re, IScopePath<S, L> path,
            Iterable<IResolutionPath<S, L, D>> specifics) throws ResolutionException, InterruptedException {
        if(Thread.interrupted()) {
            throw new InterruptedException();
        }
        final ImmutableList.Builder<IResolutionPath<S, L, D>> env = ImmutableList.builder();
        final Set.Immutable<L> max_L = max(L);
        for(L l : max_L) {
            final Set.Immutable<L> smaller = smaller(L, l);
            final List<IResolutionPath<S, L, D>> env1 = env_L(smaller, re, path, specifics);
            env.addAll(env1);
            if(env1.isEmpty() || !dataEquiv.alwaysTrue()) {
                final List<IResolutionPath<S, L, D>> env2 = env_l(l, re, path, Iterables.concat(specifics, env1));
                env.addAll(env2);
            }
        }
        return env.build();
    }

    private List<IResolutionPath<S, L, D>> env_l(L l, LabelWF<L> re, IScopePath<S, L> path,
            Iterable<IResolutionPath<S, L, D>> specifics) throws ResolutionException, InterruptedException {
        if(scopeGraph.getEdgeLabels().contains(l)) {
            return env_nonEOP(l, re, path, specifics);
        } else if(scopeGraph.getNoDataLabel().equals(l)) {
            return env_EOP(re, path, specifics);
        } else {
            throw new IllegalStateException("Encountered unknown label " + l);
        }
    }

    private List<IResolutionPath<S, L, D>> env_EOP(LabelWF<L> re, IScopePath<S, L> path,
            Iterable<IResolutionPath<S, L, D>> specifics) throws ResolutionException, InterruptedException {
        if(!re.accepting()) {
            return ImmutableList.of();
        }
        final S scope = path.getTarget();
        if(!isDataComplete.test(scope, relation)) {
            throw new IncompleteDataException(scope, relation);
        }
        final ImmutableList.Builder<IResolutionPath<S, L, D>> env = ImmutableList.builder();
        if(relation.equals(scopeGraph.getNoDataLabel())) {
            final D datum = scope;
            if(dataWF.wf(datum) && notShadowed(datum, specifics)) {
                env.add(Paths.resolve(path, relation, datum));
            }
        } else {
            for(D datum : getData(re, path, relation)) {
                if(dataWF.wf(datum) && notShadowed(datum, specifics)) {
                    env.add(Paths.resolve(path, relation, datum));
                }
            }
        }
        return env.build();
    }

    private boolean notShadowed(D datum, Iterable<IResolutionPath<S, L, D>> specifics)
            throws ResolutionException, InterruptedException {
        for(IResolutionPath<S, L, D> p : specifics) {
            if(dataEquiv.leq(p.getDatum(), datum)) {
                return false;
            }
        }
        return true;
    }

    private List<IResolutionPath<S, L, D>> env_nonEOP(L l, LabelWF<L> re, IScopePath<S, L> path,
            Iterable<IResolutionPath<S, L, D>> specifics) throws ResolutionException, InterruptedException {
        final Optional<LabelWF<L>> newRe = re.step(l);
        if(!newRe.isPresent()) {
            return ImmutableList.of();
        } else {
            re = newRe.get();
        }
        if(!isEdgeComplete.test(path.getTarget(), l)) {
            throw new IncompleteEdgeException(path.getTarget(), l);
        }
        final ImmutableList.Builder<IResolutionPath<S, L, D>> env = ImmutableList.builder();
        for(S nextScope : getEdges(re, path, l)) {
            final Optional<IScopePath<S, L>> p = Paths.append(path, Paths.edge(path.getTarget(), l, nextScope));
            if(p.isPresent()) {
                env.addAll(env(re, p.get(), specifics));
            }
        }
        return env.build();
    }

    ///////////////////////////////////////////////////////////////////////////
    // edges and data                                                        //
    ///////////////////////////////////////////////////////////////////////////

    protected Iterable<D> getData(LabelWF<L> re, IScopePath<S, L> path, L l) {
        return scopeGraph.getData(path.getTarget(), l);
    }

    protected Iterable<S> getEdges(LabelWF<L> re, IScopePath<S, L> path, L l) {
        return scopeGraph.getEdges(path.getTarget(), l);
    }

    ///////////////////////////////////////////////////////////////////////////
    // max labels                                                            //
    ///////////////////////////////////////////////////////////////////////////

    private final Map<Set.Immutable<L>, Set.Immutable<L>> maxCache = Maps.newHashMap();

    private Set.Immutable<L> max(Set.Immutable<L> L) throws ResolutionException, InterruptedException {
        Set.Immutable<L> max;
        if((max = maxCache.get(L)) == null) {
            maxCache.put(L, (max = computeMax(L)));
        }
        return max;
    }

    private Set.Immutable<L> computeMax(Set.Immutable<L> L) throws ResolutionException, InterruptedException {
        final Set.Transient<L> max = Set.Transient.of();
        outer: for(L l1 : L) {
            for(L l2 : L) {
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

    private final Map<Tuple2<Set.Immutable<L>, L>, Set.Immutable<L>> smallerCache = Maps.newHashMap();

    private Set.Immutable<L> smaller(Set.Immutable<L> L, L l1) throws ResolutionException, InterruptedException {
        Tuple2<Set.Immutable<L>, L> key = ImmutableTuple2.of(L, l1);
        Set.Immutable<L> smaller;
        if(null == (smaller = smallerCache.get(key))) {
            smallerCache.put(key, (smaller = computeSmaller(L, l1)));
        }
        return smaller;
    }

    private Set.Immutable<L> computeSmaller(Set.Immutable<L> L, L l1) throws ResolutionException, InterruptedException {
        final Set.Transient<L> smaller = Set.Transient.of();
        for(L l2 : L) {
            if(labelOrder.lt(l2, l1)) {
                smaller.__insert(l2);
            }
        }
        return smaller.freeze();
    }

    ///////////////////////////////////////////////////////////////////////////
    // builder                                                               //
    ///////////////////////////////////////////////////////////////////////////

    public static <S extends D, L, D> Builder<S, L, D> builder() {
        return new Builder<>();
    }

    public static class Builder<S extends D, L, D> implements INameResolution.Builder<S, L, D> {

        private LabelWF<L> labelWF = LabelWF.ANY();
        private LabelOrder<L> labelOrder = LabelOrder.NONE();
        private Predicate2<S, L> isEdgeComplete = (s, l) -> true;

        private DataWF<D> dataWF = DataWF.ANY();
        private DataLeq<D> dataEquiv = DataLeq.NONE();
        private Predicate2<S, L> isDataComplete = (s, r) -> true;

        @Override public Builder<S, L, D> withLabelWF(LabelWF<L> labelWF) {
            this.labelWF = labelWF;
            return this;
        }

        @Override public Builder<S, L, D> withLabelOrder(LabelOrder<L> labelOrder) {
            this.labelOrder = labelOrder;
            return this;
        }

        @Override public Builder<S, L, D> withEdgeComplete(Predicate2<S, L> isEdgeComplete) {
            this.isEdgeComplete = isEdgeComplete;
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

        @Override public Builder<S, L, D> withDataComplete(Predicate2<S, L> isDataComplete) {
            this.isDataComplete = isDataComplete;
            return this;
        }

        @Override public FastNameResolution<S, L, D> build(IScopeGraph<S, L, D> scopeGraph, L relation) {
            return new FastNameResolution<>(scopeGraph, relation, labelWF, labelOrder, isEdgeComplete, dataWF,
                    dataEquiv, isDataComplete);
        }

    }

}
