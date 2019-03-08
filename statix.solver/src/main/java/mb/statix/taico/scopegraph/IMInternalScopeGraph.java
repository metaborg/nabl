package mb.statix.taico.scopegraph;

import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.List;
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
     * @return
     *      the parent of this scope graph, or null if this is the top level scope graph
     */
    IMInternalScopeGraph<S, V, L, R> getParent();
    
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
    
    /**
     * Creates a deep copy of this scope graph, using the given scope graph as a parent.
     * 
     * @param parent
     *      the parent of the copy scope graph
     */
    IMInternalScopeGraph<S, V, L, R> deepCopy(IMInternalScopeGraph<S, V, L, R> parent);
    
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
}
