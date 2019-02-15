package mb.statix.taico.scopegraph;

import java.util.List;

import mb.statix.taico.util.IOwnable;

public interface IMInternalScopeGraph<V extends IOwnable, L, R> extends IMExternalScopeGraph<V, L, R> {
    /**
     * Gets the collection of edges from the given scope with the given label, that are
     * either edges owned by the current scope graph, or owned by any of its children.
     * 
     * @param scope
     *      the scope to start from
     * @param label
     *      the label for the edges
     * @return
     *      an iterable with all the edges
     */
    java.util.Set<IEdge<V, L, V>> getTransitiveEdges(V scope, L label);
    
    /**
     * Gets the collection of data from the given scope with the given label, that are
     * either data owned by the current scope graph, or owned by any of its children.
     * 
     * @param scope
     *      the scope to start from
     * @param label
     *      the label for the data
     * @return
     *      an iterable with all the data
     */
    java.util.Set<IEdge<V, R, List<V>>> getTransitiveData(V scope, R label);
}
