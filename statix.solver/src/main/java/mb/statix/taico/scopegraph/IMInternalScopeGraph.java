package mb.statix.taico.scopegraph;

import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.usethesource.capsule.Set.Immutable;
import mb.nabl2.util.collections.IRelation3;
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
    Set<IEdge<S, L, S>> getTransitiveEdges(S scope, L label);
    
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
    Set<IEdge<S, R, List<V>>> getTransitiveData(S scope, R label);
    
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
    IMInternalScopeGraph<S, V, L, R> createChild(IModule module, List<IOwnableScope> canExtend);
    
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
    
    List<? extends IOwnableScope> getParentScopes();
    
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
     * Deletes the given scope (e.g. for a rollback).
     * 
     * @param scope
     *      the scope to delete
     */
    void revokeScope(S scope);
    
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
    ITrackingScopeGraph<S, V, L, R> trackingGraph(Map<IModule, ITrackingScopeGraph<S, V, L, R>> trackers);
    
//    /**
//     * Copies this scope graph, using the given owner for the copy.
//     * The copy does not have any links to the old scope graph any more.
//     * 
//     * @param newOwner
//     *      the new owner
//     * 
//     * @return
//     *      a copy of this scope graph
//     * 
//     * @throws IllegalArgumentException
//     *      If the given owner does not have the same identity as the current owner.
//     */
//    IMInternalScopeGraph<S, V, L, R> copy(IModule newOwner);
    
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
}
