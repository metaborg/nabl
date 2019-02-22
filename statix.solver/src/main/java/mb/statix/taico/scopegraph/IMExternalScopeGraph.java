package mb.statix.taico.scopegraph;

import java.util.List;

import io.usethesource.capsule.Set;
import mb.statix.taico.util.IOwnable;

public interface IMExternalScopeGraph<S extends IOwnable, V, L, R> {
    
    L getEndOfPath();
    Set.Immutable<L> getLabels();
    Set.Immutable<R> getRelations();
    
    /**
     * Gets the collection of edges from the given scope with the given label.
     * 
     * @param scope
     *      the scope to start from
     * @param label
     *      the label for the edges
     * @return
     *      an iterable with all the edges
     */
    java.util.Set<IEdge<S, L, S>> getEdges(S scope, L label);
    
    /**
     * Gets the collection of data edges from the given scope with the given label.
     * 
     * @param scope
     *      the scope to start from
     * @param label
     *      the label for the edges
     * @return
     *      an iterable with all the edges
     */
    java.util.Set<IEdge<S, R, List<V>>> getData(S scope, R label);
    
    /**
     * @param sourceScope
     *      the source scope
     * @param label
     *      the label
     * @param targetScope
     *      the target scope
     * 
     * @return
     *      true if this edge was added, false if it already existed
     */
    boolean addEdge(S sourceScope, L label, S targetScope);
    
    /**
     * @param scope
     *      the scope
     * @param relation
     *      the relation
     * @param datum
     *      the datum
     * 
     * @return
     *      true if this scope graph changed as a result of this call, false otherwise
     */
    boolean addDatum(S scope, R relation, Iterable<V> datum);
    
    
    /**
     * Creates a new scope in this scope graph.
     * 
     * @param base
     *      the base string to use for the scope name
     * @return
     *      the newly created scope
     */
    S createScope(String base);
    
}
