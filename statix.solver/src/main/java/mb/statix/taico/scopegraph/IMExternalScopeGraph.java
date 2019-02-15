package mb.statix.taico.scopegraph;

import java.util.List;

import io.usethesource.capsule.Set;
import mb.statix.taico.util.IOwnable;

public interface IMExternalScopeGraph<V extends IOwnable, L, R> {
    
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
    java.util.Set<IEdge<V, L, V>> getEdges(V scope, L label);
    
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
    java.util.Set<IEdge<V, R, List<V>>> getData(V scope, R label);
    
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
    boolean addEdge(V sourceScope, L label, V targetScope);
    
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
    boolean addDatum(V scope, R relation, Iterable<V> datum);
    
    
    /**
     * Creates a new scope in this scope graph.
     * 
     * @return
     *      the newly created scope
     */
    V createScope();
    
}
