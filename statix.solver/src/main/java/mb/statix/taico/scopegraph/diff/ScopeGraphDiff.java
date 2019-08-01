package mb.statix.taico.scopegraph.diff;

import java.util.HashSet;
import java.util.Map.Entry;
import java.util.Set;

import mb.nabl2.terms.ITerm;
import mb.nabl2.util.collections.HashTrieRelation3;
import mb.nabl2.util.collections.IRelation3;
import mb.statix.scopegraph.terms.Scope;
import mb.statix.taico.name.Name;

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
    // Other
    // --------------------------------------------------------------------------------------------
    
    @Override
    public void toEffectiveDiff(DiffResult target) {
        for (Scope scope : addedEdges.keySet()) {
            ScopeGraphDiff diff = target.getOrCreateDiff(scope.getResource());
            for (Entry<ITerm, Scope> entry : addedEdges.get(scope)) {
                diff.addedEdges.put(scope, entry.getKey(), entry.getValue());
            }
        }
        
        for (Scope scope : removedEdges.keySet()) {
            ScopeGraphDiff diff = target.getOrCreateDiff(scope.getResource());
            for (Entry<ITerm, Scope> entry : removedEdges.get(scope)) {
                diff.removedEdges.put(scope, entry.getKey(), entry.getValue());
            }
        }
        
        for (Scope scope : addedData.keySet()) {
            ScopeGraphDiff diff = target.getOrCreateDiff(scope.getResource());
            for (Entry<ITerm, ITerm> entry : addedData.get(scope)) {
                diff.addedData.put(scope, entry.getKey(), entry.getValue());
            }
        }
        
        for (Scope scope : removedData.keySet()) {
            ScopeGraphDiff diff = target.getOrCreateDiff(scope.getResource());
            for (Entry<ITerm, ITerm> entry : removedData.get(scope)) {
                diff.removedData.put(scope, entry.getKey(), entry.getValue());
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
    
    /**
     * Creates a new ScopeGraphDiff which only has the addedScopes and removedScopes retained.
     * 
     * @return
     *      the new ScopeGraphDiff
     */
    public ScopeGraphDiff retainScopes() {
        return new ScopeGraphDiff(
                addedScopes, removedScopes,
                HashTrieRelation3.Transient.of(), HashTrieRelation3.Transient.of(),
                HashTrieRelation3.Transient.of(), HashTrieRelation3.Transient.of(),
                HashTrieRelation3.Transient.of(), HashTrieRelation3.Transient.of(),
                HashTrieRelation3.Transient.of());
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
    
    public static ScopeGraphDiff empty() {
        return new ScopeGraphDiff(
                new HashSet<>(), new HashSet<>(),
                HashTrieRelation3.Transient.of(), HashTrieRelation3.Transient.of(),
                HashTrieRelation3.Transient.of(), HashTrieRelation3.Transient.of(),
                HashTrieRelation3.Transient.of(), HashTrieRelation3.Transient.of(),
                HashTrieRelation3.Transient.of());
    }
}
