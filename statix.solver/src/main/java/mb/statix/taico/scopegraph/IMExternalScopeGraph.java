package mb.statix.taico.scopegraph;

import java.io.Serializable;
import java.util.concurrent.locks.Lock;

import io.usethesource.capsule.Set;
import mb.statix.scopegraph.IScopeGraph;
import mb.statix.taico.util.IOwnable;

public interface IMExternalScopeGraph<S, L, D> extends IScopeGraph<S, L, D>, IOwnable, Serializable {
    
    @Override
    L getNoDataLabel();
    
    @Override
    Set.Immutable<L> getEdgeLabels();
    
    @Override
    Set.Immutable<L> getDataLabels();
    
    /**
     * Gets the collection of edges from the given scope with the given label.
     * 
     * @param scope
     *      the scope to start from
     * @param label
     *      the label for the edges
     * 
     * @return
     *      a set with all the edges
     */
    @Override
    java.util.Set<S> getEdges(S scope, L label);
    
    /**
     * Gets the collection of data edges from the given scope with the given label.
     * 
     * @param scope
     *      the scope to start from
     * @param label
     *      the label for the edges
     * 
     * @return
     *      a set with all the edges
     */
    @Override
    java.util.Set<D> getData(S scope, L label);
    
    /**
     * @return
     *      the read lock for this scope graph (not for children)
     */
    Lock getReadLock();
}
