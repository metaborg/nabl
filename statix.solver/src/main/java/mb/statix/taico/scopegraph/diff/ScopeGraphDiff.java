package mb.statix.taico.scopegraph.diff;

import java.util.HashSet;
import java.util.Map.Entry;
import java.util.Set;

import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.unification.IUnifier;
import mb.nabl2.util.collections.HashTrieRelation3;
import mb.nabl2.util.collections.IRelation3;
import mb.statix.scopegraph.terms.Scope;
import mb.statix.taico.name.Name;
import mb.statix.taico.name.Names;
import mb.statix.taico.scopegraph.IMInternalScopeGraph;
import mb.statix.taico.solver.Context;

/**
 * Class to represent a scope graph diff.
 * <p>
 * The scope graph diff should be treated as immutable. Otherwise, issues can arise.
 * The collections returned by the different methods in this class should never be modified.
 * Modifications to this scope graph diff could affect other diffs as well.
 */
public class ScopeGraphDiff implements IScopeGraphDiff<Scope, ITerm, ITerm> {
    protected Set<Scope> addedScopes;
    protected Set<Scope> removedScopes;
    protected IRelation3.Transient<Scope, ITerm, Scope> addedEdges;
    protected IRelation3.Transient<Scope, ITerm, Scope> removedEdges;
    protected IRelation3.Transient<Scope, ITerm, ITerm> addedData;
    protected IRelation3.Transient<Scope, ITerm, ITerm> removedData;
    protected IRelation3.Transient<Scope, ITerm, Name> addedDataNames;
    protected IRelation3.Transient<Scope, ITerm, Name> removedDataNames;
    protected IRelation3.Transient<Scope, ITerm, Name> changedDataNames;
    
    public ScopeGraphDiff(
            Set<Scope> addedScopes, Set<Scope> removedScopes,
            IRelation3.Transient<Scope, ITerm, Scope> addedEdges, IRelation3.Transient<Scope, ITerm, Scope> removedEdges,
            IRelation3.Transient<Scope, ITerm, ITerm> addedData, IRelation3.Transient<Scope, ITerm, ITerm> removedData,
            IRelation3.Transient<Scope, ITerm, Name> addedDataNames, IRelation3.Transient<Scope, ITerm, Name> removedDataNames,
            IRelation3.Transient<Scope, ITerm, Name> changedDataNames) {
        this.addedScopes = addedScopes;
        this.removedScopes = removedScopes;
        this.addedEdges = addedEdges;
        this.removedEdges = removedEdges;
        this.addedData = addedData;
        this.removedData = removedData;
        this.addedDataNames = addedDataNames;
        this.removedDataNames = removedDataNames;
        this.changedDataNames = changedDataNames;
    }

    // --------------------------------------------------------------------------------------------
    // Scopes
    // --------------------------------------------------------------------------------------------
    
    @Override
    public Set<Scope> getAddedScopes() {
        return addedScopes;
    }

    @Override
    public Set<Scope> getRemovedScopes() {
        return removedScopes;
    }
    
    // --------------------------------------------------------------------------------------------
    // Edges
    // --------------------------------------------------------------------------------------------

    @Override
    public IRelation3<Scope, ITerm, Scope> getAddedEdges() {
        return addedEdges;
    }

    @Override
    public IRelation3<Scope, ITerm, Scope> getRemovedEdges() {
        return removedEdges;
    }

    // --------------------------------------------------------------------------------------------
    // Data
    // --------------------------------------------------------------------------------------------
    
    @Override
    public IRelation3<Scope, ITerm, ITerm> getAddedData() {
        return addedData;
    }

    @Override
    public IRelation3<Scope, ITerm, ITerm> getRemovedData() {
        return removedData;
    }
    
    @Override
    public IRelation3<Scope, ITerm, Name> getAddedDataNames() {
        return addedDataNames;
    }

    @Override
    public IRelation3<Scope, ITerm, Name> getRemovedDataNames() {
        return removedDataNames;
    }

    @Override
    public IRelation3<Scope, ITerm, Name> getChangedDataNames() {
        return changedDataNames;
    }
    
    // --------------------------------------------------------------------------------------------
    // Modifications
    // --------------------------------------------------------------------------------------------
    
    private void addEdge(Scope source, ITerm label, Scope target) {
        if (removedEdges.contains(source, label, target)) {
            removedEdges.remove(source, label, target);
        } else {
            addedEdges.put(source, label, target);
        }
    }
    
    private void removeEdge(Scope source, ITerm label, Scope target) {
        if (addedEdges.contains(source, label, target)) {
            addedEdges.remove(source, label, target);
        } else {
            removedEdges.put(source, label, target);
        }
    }
    
    private void addData(Scope source, ITerm label, ITerm data) {
        if (removedData.contains(source, label, data)) {
            removedData.remove(source, label, data);
        } else {
            addedData.put(source, label, data);
        }
    }
    
    private void removeData(Scope source, ITerm label, ITerm data) {
        if (addedData.contains(source, label, data)) {
            addedData.remove(source, label, data);
        } else {
            removedData.put(source, label, data);
        }
    }
    
    private void addDataName(Scope source, ITerm label, Name name) {
        if (removedDataNames.contains(source, label, name)) {
            removedDataNames.remove(source, label, name);
        } else {
            addedDataNames.put(source, label, name);
        }
    }
    
    private void removeDataName(Scope source, ITerm label, Name name) {
        if (addedDataNames.contains(source, label, name)) {
            addedDataNames.remove(source, label, name);
        } else {
            removedDataNames.put(source, label, name);
        }
    }
    
    // --------------------------------------------------------------------------------------------
    // Other
    // --------------------------------------------------------------------------------------------
    

    @Override
    public void toEffectiveDiff(DiffResult target) {
        for (Scope scope : addedEdges.keySet()) {
            ScopeGraphDiff diff = target.getOrCreateDiff(scope.getResource());
            for (Entry<ITerm, Scope> entry : addedEdges.get(scope)) {
                diff.addEdge(scope, entry.getKey(), entry.getValue());
            }
        }
        
        for (Scope scope : removedEdges.keySet()) {
            ScopeGraphDiff diff = target.getOrCreateDiff(scope.getResource());
            for (Entry<ITerm, Scope> entry : removedEdges.get(scope)) {
                diff.removeEdge(scope, entry.getKey(), entry.getValue());
            }
        }
        
        for (Scope scope : addedData.keySet()) {
            ScopeGraphDiff diff = target.getOrCreateDiff(scope.getResource());
            for (Entry<ITerm, ITerm> entry : addedData.get(scope)) {
                diff.addData(scope, entry.getKey(), entry.getValue());
            }
        }
        
        for (Scope scope : removedData.keySet()) {
            ScopeGraphDiff diff = target.getOrCreateDiff(scope.getResource());
            for (Entry<ITerm, ITerm> entry : removedData.get(scope)) {
                diff.removeData(scope, entry.getKey(), entry.getValue());
            }
        }
        
        for (Scope scope : addedDataNames.keySet()) {
            ScopeGraphDiff diff = target.getOrCreateDiff(scope.getResource());
            for (Entry<ITerm, Name> entry : addedDataNames.get(scope)) {
                diff.addedDataNames.put(scope, entry.getKey(), entry.getValue());
            }
        }
        
        for (Scope scope : removedDataNames.keySet()) {
            ScopeGraphDiff diff = target.getOrCreateDiff(scope.getResource());
            for (Entry<ITerm, Name> entry : removedDataNames.get(scope)) {
                diff.removedDataNames.put(scope, entry.getKey(), entry.getValue());
            }
        }
        
        for (Scope scope : changedDataNames.keySet()) {
            ScopeGraphDiff diff = target.getOrCreateDiff(scope.getResource());
            for (Entry<ITerm, Name> entry : changedDataNames.get(scope)) {
                diff.changedDataNames.put(scope, entry.getKey(), entry.getValue());
            }
        }
    }
    
    @Override
    public ScopeGraphDiff retainScopes() {
        return new ScopeGraphDiff(
                addedScopes, removedScopes,
                HashTrieRelation3.Transient.of(), HashTrieRelation3.Transient.of(),
                HashTrieRelation3.Transient.of(), HashTrieRelation3.Transient.of(),
                HashTrieRelation3.Transient.of(), HashTrieRelation3.Transient.of(),
                HashTrieRelation3.Transient.of());
    }
    
    @Override
    public ScopeGraphDiff inverse() {
        return new ScopeGraphDiff(
                removedScopes, addedScopes,
                removedEdges, addedEdges,
                removedData, addedData,
                removedDataNames, addedDataNames, changedDataNames
        );
    }
    
    // --------------------------------------------------------------------------------------------
    // Object methods
    // --------------------------------------------------------------------------------------------
    
    @Override
    public String toString() {
        return print();
    }
    
    // --------------------------------------------------------------------------------------------
    // Static initializers
    // --------------------------------------------------------------------------------------------
    
    /**
     * @return
     *      an empty scope graph diff
     */
    public static ScopeGraphDiff empty() {
        return new ScopeGraphDiff(
                new HashSet<>(), new HashSet<>(),
                HashTrieRelation3.Transient.of(), HashTrieRelation3.Transient.of(),
                HashTrieRelation3.Transient.of(), HashTrieRelation3.Transient.of(),
                HashTrieRelation3.Transient.of(), HashTrieRelation3.Transient.of(),
                HashTrieRelation3.Transient.of());
    }
    
    /**
     * Creates a scope graph diff for a new module.
     * 
     * @param context
     *      the context of the module
     * @param unifier
     *      the unifier of the module
     * @param scopeGraph
     *      the scope graph
     * 
     * @return
     *      the diff of the scope graph
     */
    public static ScopeGraphDiff newModule(Context context, IUnifier unifier,
            IMInternalScopeGraph<Scope, ITerm, ITerm> scopeGraph) {
        IRelation3.Transient<Scope, ITerm, Name> newDataNames = Context.executeInContext(context,
                () -> Diff.toNames(scopeGraph.getOwnData(), d -> Names.getNameOrNull(d, unifier)));
        
        return new ScopeGraphDiff(
                scopeGraph.getScopes(),                                               //+ scopes
                new HashSet<>(),                                                      //- scopes
                (IRelation3.Transient<Scope, ITerm, Scope>) scopeGraph.getOwnEdges(), //+ edges
                HashTrieRelation3.Transient.of(),                                     //- edges
                (IRelation3.Transient<Scope, ITerm, ITerm>) scopeGraph.getOwnData(),  //+ data
                HashTrieRelation3.Transient.of(),                                     //- data
                newDataNames,                                                         //+ names
                HashTrieRelation3.Transient.of(),                                     //- names
                HashTrieRelation3.Transient.of()                                      //~ names
        );
    }
    
    /**
     * Creates a scope graph diff for a removed module.
     * 
     * @param context
     *      the context of the module
     * @param unifier
     *      the unifier of the module
     * @param scopeGraph
     *      the scope graph
     * 
     * @return
     *      the diff of the scope graph
     */
    public static ScopeGraphDiff removedModule(Context context, IUnifier unifier,
            IMInternalScopeGraph<Scope, ITerm, ITerm> scopeGraph) {
        IRelation3.Transient<Scope, ITerm, Name> deletedNames = Context.executeInContext(context,
                () -> Diff.toNames(scopeGraph.getOwnData(), d -> Names.getNameOrNull(d, unifier)));
        
        return new ScopeGraphDiff(
                new HashSet<>(),                                                      //+ scopes
                scopeGraph.getScopes(),                                               //- scopes
                HashTrieRelation3.Transient.of(),                                     //+ edges
                (IRelation3.Transient<Scope, ITerm, Scope>) scopeGraph.getOwnEdges(), //- edges
                HashTrieRelation3.Transient.of(),                                     //+ data
                (IRelation3.Transient<Scope, ITerm, ITerm>) scopeGraph.getOwnData(),  //- data
                HashTrieRelation3.Transient.of(),                                     //+ names
                deletedNames,                                                         //- names
                HashTrieRelation3.Transient.of()                                      //~ names
        );
    }
}
