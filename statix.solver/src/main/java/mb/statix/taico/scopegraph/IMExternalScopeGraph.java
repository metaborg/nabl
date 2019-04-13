package mb.statix.taico.scopegraph;

import java.util.List;
import java.util.concurrent.locks.Lock;

import io.usethesource.capsule.Set;
import mb.statix.taico.scopegraph.locking.LockManager;
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
     * @param lockManager
     *      the lock manager
     * 
     * @return
     *      an iterable with all the edges
     */
    java.util.Set<IEdge<S, L, S>> getEdges(S scope, L label, LockManager lockManager);
    
    /**
     * Gets the collection of data edges from the given scope with the given label.
     * 
     * @param scope
     *      the scope to start from
     * @param label
     *      the label for the edges
     * @param lockManager
     *      the lock manager
     * 
     * @return
     *      an iterable with all the edges
     */
    java.util.Set<IEdge<S, R, List<V>>> getData(S scope, R label, LockManager lockManager);
    
    /**
     * @return
     *      the read lock for this scope graph (not for children)
     */
    Lock getReadLock();
}
