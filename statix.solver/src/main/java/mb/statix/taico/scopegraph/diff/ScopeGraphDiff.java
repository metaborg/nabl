package mb.statix.taico.scopegraph.diff;

import java.util.Map;
import java.util.Set;

import com.google.common.collect.Multimap;

import mb.nabl2.util.collections.IRelation3;
import mb.statix.taico.name.Name;
import mb.statix.taico.name.NameAndRelation;
import static mb.statix.taico.util.TPrettyPrinter.prettyPrint;

public class ScopeGraphDiff<S extends D, L, D> implements IScopeGraphDiff<S, L, D> {
    private Set<S> addedScopes;
    private Set<S> removedScopes;
    private IRelation3<S, L, S> addedEdges;
    private IRelation3<S, L, S> removedEdges;
    private IRelation3<S, L, Name> addedData;
    private IRelation3<S, L, Name> removedData;
    private IRelation3<S, L, Name> changedData;
    
    public ScopeGraphDiff(Set<S> addedScopes, Set<S> removedScopes, IRelation3<S, L, S> addedEdges,
            IRelation3<S, L, S> removedEdges, IRelation3<S, L, Name> addedData, IRelation3<S, L, Name> removedData,
            IRelation3<S, L, Name> changedData) {
        this.addedScopes = addedScopes;
        this.removedScopes = removedScopes;
        this.addedEdges = addedEdges;
        this.removedEdges = removedEdges;
        this.addedData = addedData;
        this.removedData = removedData;
        this.changedData = changedData;
    }

    @Override
    public Set<S> getAddedScopes() {
        return addedScopes;
    }

    @Override
    public Set<S> getRemovedScopes() {
        return removedScopes;
    }

    @Override
    public Multimap<S, NameAndRelation> getChangedNamesPS() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Multimap<S, NameAndRelation> getRemovedNamesPS() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Multimap<S, NameAndRelation> getAddedNamesPS() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public IRelation3<S, L, S> getAddedEdges() {
        return addedEdges;
    }

    @Override
    public IRelation3<S, L, S> getRemovedEdges() {
        return removedEdges;
    }

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

    @Override
    public Map<String, IScopeGraphDiff<S, L, D>> childDiffs() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String toString() {
        return "ScopeGraphDiff [addedScopes=" + prettyPrint(addedScopes)
                + ",\n removedScopes=" + prettyPrint(removedScopes)
                + ",\n addedEdges=" + prettyPrint(addedEdges)
                + ",\n removedEdges=" + prettyPrint(removedEdges)
                + ",\n addedData=" + prettyPrint(addedData)
                + ",\n removedData=" + prettyPrint(removedData)
                + ",\n changedData=" + prettyPrint(changedData) + "]";
    }

    
}
