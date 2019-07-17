package mb.statix.taico.scopegraph.diff;

import java.util.Set;

import mb.nabl2.util.collections.IRelation3;
import mb.statix.taico.name.Name;

public class ScopeGraphDiff<S extends D, L, D> implements IScopeGraphDiff<S, L, D> {
    private Set<S> addedScopes;
    private Set<S> removedScopes;
    private IRelation3<S, L, S> addedEdges;
    private IRelation3<S, L, S> removedEdges;
    private IRelation3<S, L, Name> addedData;
    private IRelation3<S, L, Name> removedData;
    private IRelation3<S, L, Name> changedData;
    
    public ScopeGraphDiff(
            Set<S> addedScopes, Set<S> removedScopes,
            IRelation3<S, L, S> addedEdges, IRelation3<S, L, S> removedEdges,
            IRelation3<S, L, Name> addedData, IRelation3<S, L, Name> removedData,
            IRelation3<S, L, Name> changedData) {
        this.addedScopes = addedScopes;
        this.removedScopes = removedScopes;
        this.addedEdges = addedEdges;
        this.removedEdges = removedEdges;
        this.addedData = addedData;
        this.removedData = removedData;
        this.changedData = changedData;
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
    public IRelation3<S, L, Name> getAddedData() {
        return addedData;
    }

    @Override
    public IRelation3<S, L, Name> getRemovedData() {
        return removedData;
    }

    @Override
    public IRelation3<S, L, Name> getChangedData() {
        return changedData;
    }
    
    // --------------------------------------------------------------------------------------------
    // Object methods
    // --------------------------------------------------------------------------------------------
    
    @Override
    public String toString() {
        return print();
    }
}
