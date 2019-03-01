package mb.statix.taico.scopegraph;

import java.util.List;

import mb.statix.taico.module.IModule;
import mb.statix.taico.util.IOwnable;

public interface IMInternalScopeGraph<S extends IOwnable, V, L, R> extends IMExternalScopeGraph<S, V, L, R> {
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
    java.util.Set<IEdge<S, L, S>> getTransitiveEdges(S scope, L label);
    
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
    java.util.Set<IEdge<S, R, List<V>>> getTransitiveData(S scope, R label);
    
    /**
     * Creates a child scope graph from this scope graph.
     * 
     * @param module
     *      the module that will own the child graph
     * @param canExtend
     *      the scopes that this child can extend
     * @return
     *      the new scope graph
     */
    IMInternalScopeGraph<S, V, L, R> createChild(IModule module, Iterable<IOwnableScope> canExtend);
}
