package mb.statix.taico.scopegraph;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.Lock;

import mb.nabl2.terms.ITerm;
import mb.nabl2.util.collections.IRelation3;
import mb.statix.scopegraph.terms.Scope;
import mb.statix.taico.module.IModule;
import mb.statix.taico.scopegraph.locking.LockManager;

public interface IMInternalScopeGraph<S, L, D> extends IMExternalScopeGraph<S, L, D> {
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
    boolean addDatum(S scope, L relation, D datum);
    
    
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
     * 
     * @return
     *      a set with all the edges
     */
    Set<S> getTransitiveEdges(S scope, L label);
    
    /**
     * Gets the collection of data from the given scope with the given label, that are
     * either data owned by the current scope graph, or owned by any of its children.
     * 
     * @param scope
     *      the scope to start from
     * @param label
     *      the label for the data
     * 
     * @return
     *      a set with all the data
     */
    Set<D> getTransitiveData(S scope, L label);
    
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
    IMInternalScopeGraph<S, L, D> createChild(IModule module, List<S> canExtend);
    
    /**
     * Adds the given module as a child to this scope graph.
     * 
     * @param child
     *      the child module
     * 
     * @return
     *      the child scope graph
     */
    IMInternalScopeGraph<S, L, D> addChild(IModule child);
    
    /**
     * @return
     *      an iterable of all the children of this scope graph
     */
    Iterable<? extends IMInternalScopeGraph<S, L, D>> getChildren();
    
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
    IRelation3<S, L, S> getOwnEdges();
    
    /**
     * @return
     *      a relation of all the data edges directly owned by this module
     */
    IRelation3<S, L, D> getOwnData();
    
    /**
     * @return
     *      a set of all the scopes owned by this module directly
     */
    Set<S> getScopes();
    
    /**
     * @return
     *      the set of scopes that can be extended by this scope graph
     */
    io.usethesource.capsule.Set.Immutable<? extends S> getExtensibleScopes();
    
    /**
     * @return
     *      the list of parent scopes, in the order we received them
     */
    List<? extends S> getParentScopes();
    
    /**
     * @return
     *      a new tracking graph for this scope graph
     */
    ITrackingScopeGraph<S, L, D> trackingGraph();
    
    /**
     * When creating tracking graphs from one view, all views must share the same set of trackers.
     * As such, the passed map is the reference to the single map that contains all trackers.
     * 
     * @param trackers
     *      a map with the trackers of the current view
     * @param lockManager
     *      the lock manager of the trackers
     * 
     * @return
     *      a new tracking graph for this scope graph
     */
    ITrackingScopeGraph<S, L, D> trackingGraph(Map<String, ITrackingScopeGraph<S, L, D>> trackers, LockManager lockManager);
    
    /**
     * @return
     *      a new locking graph for this scope graph
     */
    ILockingScopeGraph<S, L, D> lockingGraph();
    
    /**
     * When creating locking graphs from one view, all views must share the same set of graphs.
     * As such, the passed map is the reference to the single map that contains all graphs.
     * 
     * @param graphs
     *      a map with the graphs of the current view
     * @param lockManager
     *      the lock manager of the graphs
     * 
     * @return
     *      a new locking graph for this scope graph
     */
    ILockingScopeGraph<S, L, D> lockingGraph(Map<String, ILockingScopeGraph<S, L, D>> graphs, LockManager lockManager);
    
    /**
     * @return
     *      an external view on this scope graph
     */
    IMExternalScopeGraph<S, L, D> externalGraph();
    
    /**
     * @param clearScopes
     *      if true, scopes of the original are cleared in the delegate
     * 
     * @return
     *      a delegating scope graph
     */
    IMInternalScopeGraph<S, L, D> delegatingGraph(boolean clearScopes);
    
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

    
    /**
     * @param module
     *      the new owner
     * 
     * @return
     *      the copy
     */
    IMInternalScopeGraph<Scope, ITerm, ITerm> copy(IModule module);
}
