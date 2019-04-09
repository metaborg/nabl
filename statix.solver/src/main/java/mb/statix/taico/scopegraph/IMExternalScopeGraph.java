package mb.statix.taico.scopegraph;

import java.util.List;

import io.usethesource.capsule.Set;
import mb.statix.taico.util.IOwnable;

public interface IMExternalScopeGraph<S extends IOwnable, V, L, R> extends IOwnable {
    
    L getEndOfPath();
    Set.Immutable<? extends L> getLabels();
    Set.Immutable<? extends R> getRelations();
    
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
    
}
