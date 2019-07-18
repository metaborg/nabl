package mb.statix.taico.scopegraph.diff;

import java.util.Set;

import mb.nabl2.util.collections.IRelation3;
import mb.statix.taico.name.Name;

public class ScopeGraphDiff<S extends D, L, D> implements IScopeGraphDiff<S, L, D> {
    private Set<S> addedScopes;
    private Set<S> removedScopes;
    private IRelation3<S, L, S> addedEdges;
    private IRelation3<S, L, S> removedEdges;
    private IRelation3<S, L, D> addedData;
    private IRelation3<S, L, D> removedData;
    private IRelation3<S, L, Name> addedDataNames;
    private IRelation3<S, L, Name> removedDataNames;
    private IRelation3<S, L, Name> changedDataNames;
    
    public ScopeGraphDiff(
            Set<S> addedScopes, Set<S> removedScopes,
            IRelation3<S, L, S> addedEdges, IRelation3<S, L, S> removedEdges,
            IRelation3<S, L, D> addedData, IRelation3<S, L, D> removedData,
            IRelation3<S, L, Name> addedDataNames, IRelation3<S, L, Name> removedDataNames,
            IRelation3<S, L, Name> changedDataNames) {
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
    public Set<S> getAddedScopes() {
        return addedScopes;
    }

    @Override
    public Set<S> getRemovedScopes() {
        return removedScopes;
    }
    
    // --------------------------------------------------------------------------------------------
    // Edges
    // --------------------------------------------------------------------------------------------

    @Override
    public IRelation3<S, L, S> getAddedEdges() {
        return addedEdges;
    }

    @Override
    public IRelation3<S, L, S> getRemovedEdges() {
        return removedEdges;
    }

    // --------------------------------------------------------------------------------------------
    // Data
    // --------------------------------------------------------------------------------------------
    
    @Override
    public IRelation3<S, L, D> getAddedData() {
        return addedData;
    }

    @Override
    public IRelation3<S, L, D> getRemovedData() {
        return removedData;
    }
    
    @Override
    public IRelation3<S, L, Name> getAddedDataNames() {
        return addedDataNames;
    }

    @Override
    public IRelation3<S, L, Name> getRemovedDataNames() {
        return removedDataNames;
    }

    @Override
    public IRelation3<S, L, Name> getChangedDataNames() {
        return changedDataNames;
    }
    
    // --------------------------------------------------------------------------------------------
    // Object methods
    // --------------------------------------------------------------------------------------------
    
    @Override
    public String toString() {
        return print();
    }
}
