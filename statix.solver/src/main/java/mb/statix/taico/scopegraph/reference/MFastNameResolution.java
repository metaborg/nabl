package mb.statix.taico.scopegraph.reference;

import java.util.Map;
import java.util.Optional;

import org.metaborg.util.functions.Function2;

import com.google.common.collect.Maps;

import io.usethesource.capsule.Set;
import mb.nabl2.util.ImmutableTuple2;
import mb.nabl2.util.Tuple2;
import mb.statix.scopegraph.INameResolution;
import mb.statix.scopegraph.path.IResolutionPath;
import mb.statix.scopegraph.path.IScopePath;
import mb.statix.scopegraph.reference.DataLeq;
import mb.statix.scopegraph.reference.DataWF;
import mb.statix.scopegraph.reference.IncompleteDataException;
import mb.statix.scopegraph.reference.IncompleteEdgeException;
import mb.statix.scopegraph.reference.LabelOrder;
import mb.statix.scopegraph.reference.LabelWF;
import mb.statix.scopegraph.reference.ResolutionException;
import mb.statix.scopegraph.terms.path.Paths;
import mb.statix.taico.scopegraph.IMInternalScopeGraph;
import mb.statix.taico.solver.CompletenessResult;

public class MFastNameResolution<S extends D, L, D> implements INameResolution<S, L, D> {

    private final IMInternalScopeGraph<S, L, D> scopeGraph;
    private final Set.Immutable<L> labels;
    private final L relation;

    private final LabelWF<L> labelWF; // default: true
    private final LabelOrder<L> labelOrder; // default: false
    private final Function2<? super S, L, CompletenessResult> isEdgeComplete; // default: true

    private final DataWF<D> dataWF; // default: true
    private final DataLeq<D> dataEquiv; // default: false
    private final Function2<? super S, L, CompletenessResult> isDataComplete; // default: true

    public MFastNameResolution(IMInternalScopeGraph<S, L, D> scopeGraph, L relation, LabelWF<L> labelWF,
            LabelOrder<L> labelOrder, Function2<? super S, L, CompletenessResult> isEdgeComplete, DataWF<D> dataWF, DataLeq<D> dataEquiv,
            Function2<? super S, L, CompletenessResult> isDataComplete) {
        super();
        this.scopeGraph = scopeGraph;
        this.labels = Set.Immutable.<L>of().__insertAll(scopeGraph.getEdgeLabels()).__insert(scopeGraph.getNoDataLabel());
        this.relation = relation;
        this.labelWF = labelWF;
        this.labelOrder = labelOrder;
        this.isEdgeComplete = isEdgeComplete;
        this.dataWF = dataWF;
        this.dataEquiv = dataEquiv;
        this.isDataComplete = isDataComplete;
    }
    
    //TODO We want to eventually switch to a derivative query scenario. We need to know per scope what types of edges we are interested in?

    @Override public java.util.Set<IResolutionPath<S, L, D>> resolve(S scope)
            throws ResolutionException, InterruptedException {
        return env(labelWF, Paths.empty(scope), Set.Immutable.of());
    }

    private Set<IResolutionPath<S, L, D>> env(LabelWF<L> re, IScopePath<S, L> path,
            Set.Immutable<IResolutionPath<S, L, D>> specifics) throws ResolutionException, InterruptedException {
        return env_L(labels, re, path, specifics);
    }

    // FIXME Use caching of single label environments to prevent recalculation in case of diamonds in
    // the graph
    private Set.Immutable<IResolutionPath<S, L, D>> env_L(Set.Immutable<L> L, LabelWF<L> re, IScopePath<S, L> path,
            Set.Immutable<IResolutionPath<S, L, D>> specifics) throws ResolutionException, InterruptedException {
        if(Thread.interrupted()) {
            throw new InterruptedException();
        }
        final Set.Transient<IResolutionPath<S, L, D>> env = Set.Transient.of();
        final Set<L> max_L = max(L);
        for(L l : max_L) {
            final Set.Immutable<IResolutionPath<S, L, D>> env1 = env_L(smaller(L, l), re, path, specifics);
            env.__insertAll(env1);
            if(env1.isEmpty() || !dataEquiv.alwaysTrue()) {
                final Set.Immutable<IResolutionPath<S, L, D>> env2 =
                        env_l(l, re, path, Set.Immutable.union(specifics, env1));
                env.__insertAll(env2);
            }
        }
        return env.freeze();
    }

    private Set.Immutable<IResolutionPath<S, L, D>> env_l(L l, LabelWF<L> re, IScopePath<S, L> path,
            Set.Immutable<IResolutionPath<S, L, D>> specifics) throws ResolutionException, InterruptedException {
        return l.equals(scopeGraph.getNoDataLabel()) ? env_EOP(re, path, specifics) : env_nonEOP(l, re, path, specifics);
    }

    private Set.Immutable<IResolutionPath<S, L, D>> env_EOP(LabelWF<L> re, IScopePath<S, L> path,
            Set.Immutable<IResolutionPath<S, L, D>> specifics) throws ResolutionException, InterruptedException {
        if(!re.accepting()) {
            return Set.Immutable.of();
        }
        final S scope = path.getTarget();
        CompletenessResult result = isDataComplete.apply(scope, relation);
        if(result != null) {
            if(!result.isComplete()) {
                throw new IncompleteDataException(scope, relation, result.cause());
            }
        }
        final Set.Transient<IResolutionPath<S, L, D>> env = Set.Transient.of();
        if(relation != null) {
            for(D datum : scopeGraph.getData(path.getTarget(), relation)) {
                if(dataWF.wf(datum) && notShadowed(datum, specifics)) {
                    env.__insert(Paths.resolve(path, relation, datum));
                }
            }
        } else {
            final D datum = scope;
            if(dataWF.wf(datum) && notShadowed(datum, specifics)) {
                env.__insert(Paths.resolve(path, relation, datum));
            }
        }
        return env.freeze();
    }

    private boolean notShadowed(D datum, Set.Immutable<IResolutionPath<S, L, D>> specifics)
            throws ResolutionException, InterruptedException {
        for(IResolutionPath<S, L, D> p : specifics) {
            if(dataEquiv.leq(p.getDatum(), datum)) {
                return false;
            }
        }
        return true;
    }

    private Set.Immutable<IResolutionPath<S, L, D>> env_nonEOP(L l, LabelWF<L> re, IScopePath<S, L> path,
            Set.Immutable<IResolutionPath<S, L, D>> specifics) throws ResolutionException, InterruptedException {
        final Optional<LabelWF<L>> newRe = re.step(l);
        if(!newRe.isPresent()) {
            return Set.Immutable.of();
        }
        CompletenessResult result = isEdgeComplete.apply(path.getTarget(), l);
        if(!result.isComplete()) {
            throw new IncompleteEdgeException(path.getTarget(), l, result.cause());
        }
        final Set.Transient<IResolutionPath<S, L, D>> env = Set.Transient.of();
        for(S nextScope : scopeGraph.getEdges(path.getTarget(), l)) {
            final Optional<IScopePath<S, L>> p = Paths.append(path, Paths.edge(path.getTarget(), l, nextScope));
            if(p.isPresent()) {
                env.__insertAll(env(newRe.get(), p.get(), specifics));
            }
        }
        return env.freeze();
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
        if((smaller = smallerCache.get(key)) == null) {
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
    //////////////////////////////////////////////////////////////////S/////////

    public static <S extends D, L, D> Builder<S, L, D> builder() {
        return new Builder<>(); 
    }

    public static class Builder<S extends D, L, D> {

        private LabelWF<L> labelWF = LabelWF.ANY();
        private LabelOrder<L> labelOrder = LabelOrder.NONE();
        private Function2<? super S, L, CompletenessResult> isEdgeComplete = (s, l) -> CompletenessResult.of(true, null);

        private DataWF<D> dataWF = DataWF.ANY();
        private DataLeq<D> dataEquiv = DataLeq.NONE();
        private Function2<? super S, L, CompletenessResult> isDataComplete = (s, r) -> CompletenessResult.of(true, null);

        public Builder<S, L, D> withLabelWF(LabelWF<L> labelWF) {
            this.labelWF = labelWF;
            return this;
        }

        public Builder<S, L, D> withLabelOrder(LabelOrder<L> labelOrder) {
            this.labelOrder = labelOrder;
            return this;
        }

        public Builder<S, L, D> withEdgeComplete(Function2<? super S, L, CompletenessResult> isEdgeComplete) {
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

        public Builder<S, L, D> withDataComplete(Function2<? super S, L, CompletenessResult> isDataComplete) {
            this.isDataComplete = isDataComplete;
            return this;
        }
        
        public MFastNameResolution<S, L, D> build(IMInternalScopeGraph<S, L, D> scopeGraph, L relation) {
            return new MFastNameResolution<>(scopeGraph, relation, labelWF, labelOrder, isEdgeComplete, dataWF,
                    dataEquiv, isDataComplete);
        }

    }

}