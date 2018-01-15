package org.metaborg.meta.nabl2.scopegraph.esop;

import org.metaborg.meta.nabl2.scopegraph.ILabel;
import org.metaborg.meta.nabl2.scopegraph.INameResolution;
import org.metaborg.meta.nabl2.scopegraph.IOccurrence;
import org.metaborg.meta.nabl2.scopegraph.IResolutionParameters;
import org.metaborg.meta.nabl2.scopegraph.IScope;
import org.metaborg.meta.nabl2.scopegraph.esop.persistent.AllShortestPathsNameResolution;
import org.metaborg.meta.nabl2.scopegraph.esop.persistent.BiSimulationNameResolution;
import org.metaborg.meta.nabl2.scopegraph.esop.persistent.PersistentNameResolution;
import org.metaborg.meta.nabl2.scopegraph.esop.persistent.PersistentScopeGraph;
import org.metaborg.meta.nabl2.scopegraph.esop.reference.EsopNameResolution;
import org.metaborg.meta.nabl2.scopegraph.esop.reference.EsopScopeGraph;
import org.metaborg.meta.nabl2.scopegraph.path.IResolutionPath;
import org.metaborg.util.functions.Predicate2;
import org.metaborg.util.iterators.Iterables2;

import com.google.common.annotations.Beta;
import com.google.common.collect.SetMultimap;

@Beta
public interface IEsopNameResolution<S extends IScope, L extends ILabel, O extends IOccurrence>
        extends INameResolution<S, L, O> {

    public static final boolean USE_PERSISTENT_SCOPE_GRAPH = Boolean.getBoolean("usePersistentScopeGraph");
    
    /*
     * Factory method to switch between different scope graph implementations.
     * TODO: decide on type: IEsopScopeGraph is either persistent or transient
     */
    static <S extends IScope, L extends ILabel, O extends IOccurrence> IEsopNameResolution.Transient<S, L, O>
            builder(IResolutionParameters<L> params, IEsopScopeGraph<S, L, O, ?> scopeGraph, Predicate2<S, L> isEdgeClosed) {
        if(USE_PERSISTENT_SCOPE_GRAPH) {
            // return PersistentNameResolution.builder(params, scopeGraph, isEdgeClosed);

            IEsopNameResolution.Immutable<S, L, O> one = EsopNameResolution.Immutable.of(params, scopeGraph);
            // IEsopNameResolution.Immutable<S, L, O> two = new PersistentNameResolution<>(scopeGraph, params, isEdgeClosed);
            IEsopNameResolution.Immutable<S, L, O> two = new AllShortestPathsNameResolution<>(scopeGraph, params, isEdgeClosed);           
            
            // return new BiSimulationNameResolution<>(one, two).melt(scopeGraph, isEdgeClosed);            
            // return one.melt(scopeGraph, isEdgeClosed);
            return two.melt(scopeGraph, isEdgeClosed);
        } else {
            return EsopNameResolution.Transient.of(params, scopeGraph, isEdgeClosed);
        }
    }
        
    /*
     * NOTE: do not use; temporarily used for debugging
     */
    @Beta
    @Deprecated
    IResolutionParameters<L> getResolutionParameters();
    
    /*
     * NOTE: do not use; temporarily used for debugging
     */
    @Beta
    @Deprecated
    IEsopScopeGraph<S, L, O, ?> getScopeGraph();
    
    /*
     * NOTE: do not use; temporarily used for debugging
     */
    @Beta
    @Deprecated
    // BiPredicate<S, L> edgeClosedPredicate();
    boolean isEdgeClosed(S scope, L label);
    
    interface Immutable<S extends IScope, L extends ILabel, O extends IOccurrence>
            extends IEsopNameResolution<S, L, O>, INameResolution.Immutable<S, L, O> {

        IEsopNameResolution.Transient<S, L, O> melt(IEsopScopeGraph<S, L, O, ?> scopeGraph,
                Predicate2<S, L> isEdgeClosed);

    }

    interface Transient<S extends IScope, L extends ILabel, O extends IOccurrence>
            extends IEsopNameResolution<S, L, O>, INameResolution.Transient<S, L, O> {

        boolean addAll(IEsopNameResolution<S, L, O> other);

        default void resolveAll(Iterable<? extends O> refs) {
            Iterables2.stream(refs).forEach(this::resolve);
        }

        IEsopNameResolution.Immutable<S, L, O> freeze();

    }

    interface Update<S extends IScope, L extends ILabel, O extends IOccurrence> {

        SetMultimap<O, IResolutionPath<S, L, O>> resolved();

        SetMultimap<S, O> visible();

        SetMultimap<S, O> reachable();

        default boolean isEmpty() {
            return resolved().isEmpty() && visible().isEmpty() && reachable().isEmpty();
        }

    }

}