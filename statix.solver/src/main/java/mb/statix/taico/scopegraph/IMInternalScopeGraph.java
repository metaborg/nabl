package mb.statix.taico.scopegraph;

import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.Lock;

import io.usethesource.capsule.Set.Immutable;
import mb.nabl2.util.collections.IRelation3;
import mb.statix.taico.module.IModule;
import mb.statix.taico.scopegraph.locking.LockManager;

public interface IMInternalScopeGraph<S, V, L, R> extends IMExternalScopeGraph<S, V, L, R> {
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
    
    /**
     * Gets the collection of edges from the given scope with the given label, that are
     * either edges owned by the current scope graph, or owned by any of its children.
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
    Set<IEdge<S, L, S>> getTransitiveEdges(S scope, L label, LockManager lockManager);
    
    /**
     * Gets the collection of data from the given scope with the given label, that are
     * either data owned by the current scope graph, or owned by any of its children.
     * 
     * @param scope
     *      the scope to start from
     * @param label
     *      the label for the data
     * @param lockManager
     *      the lock manager
     * 
     * @return
     *      an iterable with all the data
     */
    Set<IEdge<S, R, List<V>>> getTransitiveData(S scope, R label, LockManager lockManager);
    
    //Scope graph tree
    
    /**
     * Creates a child scope graph from this scope graph.
     * 
     * @param module
     *      the module that will own the child graph
     * @param canExtend
     *      the scopes that this child can extend, in the order they are encountered
     * @return
     *      the new scope graph
     */
    IMInternalScopeGraph<S, V, L, R> createChild(IModule module, List<S> canExtend);
    
    /**
     * Adds the given module as a child to this scope graph.
     * 
     * @param child
     *      the child module
     * 
     * @return
     *      the child scope graph
     */
    IMInternalScopeGraph<S, V, L, R> addChild(IModule child);
    
    /**
     * @return
     *      a collection of all the children of this scope graph
     */
    Collection<? extends IMInternalScopeGraph<S, V, L, R>> getChildren();
    
    /**
     * Removes the given child module.
     * 
     * @param child
     *      the child module to remove
     * 
     * @return
     *      true if this scope graph changed as a result of this call, false otherwise
     */
    boolean removeChild(IModule child);
    
    /**
     * Removes all children of this module (transitively).
     */
    void purgeChildren();
    
    //Getters for the internal data structures of the scope graph
    
    /**
     * @return
     *      a relation of all the edges directly owned by this module
     */
    IRelation3<S, L, IEdge<S, L, S>> getEdges();
    
    /**
     * @return
     *      a relation of all the data edges directly owned by this module
     */
    IRelation3<S, R, IEdge<S, R, List<V>>> getData();
    
    /**
     * @return
     *      a set of all the scopes owned by this module directly
     */
    Set<? extends S> getScopes();
    
    /**
     * @return
     *      the set of scopes that can be extended by this scope graph
     */
    Immutable<? extends S> getExtensibleScopes();
    
    /**
     * @return
     *      the list of parent scopes, in the order we received them
     */
    List<? extends S> getParentScopes();
    
    /**
     * Creates a deep copy of this scope graph.
     */
    IMInternalScopeGraph<S, V, L, R> deepCopy();
    
    /**
     * Updates this scope graph to the state of the given copy.
     * 
     * @param copy
     *      the copy of this scope graph
     * @param checkConcurrency
     *      
     * 
     * @throws IllegalArgumentException
     *      If the given scope graph is not a copy of this one.
     * @throws ConcurrentModificationException
     *      If this scope graph has been updated after the copy was created.
     */
    void updateToCopy(IMInternalScopeGraph<S, V, L, R> copy, boolean checkConcurrency);
    
    /**
     * @return
     *      a new tracking graph for this scope graph
     */
    ITrackingScopeGraph<S, V, L, R> trackingGraph();
    
    /**
     * When creating tracking graphs from one view, all views must share the same set of trackers.
     * As such, the passed map is the reference to the single map that contains all trackers.
     * 
     * @param trackers
     *      a map with the trackers of the current view
     * 
     * @return
     *      a new tracking graph for this scope graph
     */
    ITrackingScopeGraph<S, V, L, R> trackingGraph(Map<String, ITrackingScopeGraph<S, V, L, R>> trackers);
    
    /**
     * @return
     *      an external view on this scope graph
     */
    IMExternalScopeGraph<S, V, L, R> externalGraph();
    
    /**
     * @param clearScopes
     *      if true, scopes of the original are cleared in the delegate
     * 
     * @return
     *      a delegating scope graph
     */
    IMInternalScopeGraph<S, V, L, R> delegatingGraph(boolean clearScopes);
    
    /**
     * Substitutes old parent scopes (extensible scopes) with the given new scopes.
     * 
     * @deprecated Should be replaced by a non substituting method.
     * 
     * @param newScopes
     *      the new scopes
     * 
     * @throws IllegalArgumentException
     *      If the number of new scopes is not the same as the number of old scopes.
     */
    @Deprecated
    void substitute(List<? extends S> newScopes);
    
    /**
     * @return
     *      the write lock for this scope graph (not for children)
     */
    Lock getWriteLock();
}