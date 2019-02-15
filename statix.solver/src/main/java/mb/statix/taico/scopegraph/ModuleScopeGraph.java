package mb.statix.taico.scopegraph;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import io.usethesource.capsule.Set;
import mb.nabl2.util.collections.HashTrieRelation3;
import mb.nabl2.util.collections.IRelation3;
import mb.statix.taico.module.IModule;
import mb.statix.taico.util.IOwnable;

public class ModuleScopeGraph<V extends IOwnable<V, L, R>, L, R> implements IMScopeGraph<V, L, R>, IOwnable<V, L, R> {
    private final IModule<V, L, R> owner;
    private final Set.Immutable<L> labels;
    private final L endOfPath;
    private final Set.Immutable<R> relations;
    private HashSet<V> scopes = new HashSet<>();
    
    private IRelation3.Transient<V, L, IEdge<V, L, V>> edges = HashTrieRelation3.Transient.of();
    private IRelation3.Transient<V, R, IEdge<V, R, List<V>>> data = HashTrieRelation3.Transient.of();

    public ModuleScopeGraph(
            IModule<V, L, R> owner,
            Set.Immutable<L> labels,
            L endOfPath,
            Set.Immutable<R> relations) {
        this.owner = owner;
        this.labels = labels;
        this.endOfPath = endOfPath;
        assert labels.contains(endOfPath);
        this.relations = relations;
    }
    
    @Override
    public Set.Immutable<L> getLabels() {
        return labels;
    }

    @Override
    public Set.Immutable<R> getRelations() {
        return relations;
    }
    
    @Override
    public Iterable<V> getScopes() {
        return scopes;
    }

    @Override
    public Iterable<IEdge<V, L, V>> getEdges(V scope, L label) {
        return edges.get(scope, label);
    }

    @Override
    public Iterable<IEdge<V, R, List<V>>> getData(V scope, R label) {
        return data.get(scope, label);
    }

    @Override
    public boolean addEdge(V sourceScope, L label, V targetScope) {
        if (sourceScope.getOwner() != this.owner) {
            throw new IllegalArgumentException(
                    "addEdge directed to wrong scope: "
                    + "source scope module mismatch, "
                    + "my module (" + this.owner + ") "
                    + "is not source module (" + sourceScope.getOwner() + ")");
        }
        
        IEdge<V, L, V> edge = new Edge<>(this.owner, sourceScope, label, targetScope);
        return edges.put(sourceScope, label, edge);
    }

    @Override
    public boolean addDatum(V scope, R relation, Iterable<V> datum) {
        if (scope.getOwner() != this.owner) {
            System.out.println("Adding datum edge from unowned scope (" + scope.getOwner() + ") in " + this.owner);
        }
        
        ArrayList<V> datumlist = new ArrayList<>();
        for (V v : datum) {
            datumlist.add(v);
        }
        
        IEdge<V, R, List<V>> edge = new Edge<>(this.owner, scope, relation, datumlist);
        return data.put(scope, relation, edge);
    }
    
    @Override
    public IModule<V, L, R> getOwner() {
        return this.owner;
    }
}
