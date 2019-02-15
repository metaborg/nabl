package mb.statix.taico.scopegraph;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import io.usethesource.capsule.Set;
import mb.nabl2.terms.ITerm;
import mb.nabl2.util.collections.HashTrieRelation3;
import mb.nabl2.util.collections.IRelation3;
import mb.statix.taico.module.IModule;
import mb.statix.taico.util.IOwnable;

public class ModuleScopeGraph implements IMInternalScopeGraph<IOwnableScope, ITerm, ITerm>, IOwnable {
    private final IModule owner;
    private final Set.Immutable<ITerm> labels;
    private final ITerm endOfPath;
    private final Set.Immutable<ITerm> relations;
    /** Scopes from parent that you can extend. Used for checking if an edge addition is valid. */
    private final Set.Immutable<IOwnableScope> canExtend;
    private HashSet<IOwnableScope> scopes = new HashSet<>();
    
    private IRelation3.Transient<IOwnableScope, ITerm, IEdge<IOwnableScope, ITerm, IOwnableScope>> edges = HashTrieRelation3.Transient.of();
    private IRelation3.Transient<IOwnableScope, ITerm, IEdge<IOwnableScope, ITerm, List<IOwnableScope>>> data = HashTrieRelation3.Transient.of();

    public ModuleScopeGraph(
            IModule owner,
            Set.Immutable<ITerm> labels,
            ITerm endOfPath,
            Set.Immutable<ITerm> relations,
            Set.Immutable<IOwnableScope> canExtend) {
        this.owner = owner;
        this.labels = labels;
        this.endOfPath = endOfPath;
        assert labels.contains(endOfPath);
        this.relations = relations;
        this.canExtend = canExtend;
        //TODO Children?
    }
    
    @Override
    public ITerm getEndOfPath() {
        return this.endOfPath;
    }
    
    @Override
    public Set.Immutable<ITerm> getLabels() {
        return labels;
    }

    @Override
    public Set.Immutable<ITerm> getRelations() {
        return relations;
    }
    
    public Iterable<IOwnableScope> getScopes() {
        return scopes;
    }

    @Override
    public java.util.Set<IEdge<IOwnableScope, ITerm, IOwnableScope>> getEdges(IOwnableScope scope, ITerm label) {
        if (scope.getOwner() == this.owner) {
            return getTransitiveEdges(scope, label);
        } else {
            return scope.getOwner().getScopeGraph().getEdges(scope, label);
        }
    }
    
    @Override
    public java.util.Set<IEdge<IOwnableScope, ITerm, List<IOwnableScope>>> getData(IOwnableScope scope, ITerm label) {
        return data.get(scope, label);
    }
    
    @Override
    public java.util.Set<IEdge<IOwnableScope, ITerm, IOwnableScope>> getTransitiveEdges(IOwnableScope scope, ITerm label) {
        // OPTIMIZE Only query children if they can extend scope (currently O(n) in number of modules) 
        // TODO relevant for dependency determination?
        java.util.Set<IEdge<IOwnableScope, ITerm, IOwnableScope>> set = new HashSet<>();
        set.addAll(edges.get(scope, label));
        for (IModule child : owner.getChildren()) {
            set.addAll(child.getScopeGraph().getTransitiveEdges(scope, label));
        }
        return set;
    }

    @Override
    public java.util.Set<IEdge<IOwnableScope, ITerm, List<IOwnableScope>>> getTransitiveData(IOwnableScope scope, ITerm label) {
        // OPTIMIZE Only query children if they can extend scope (currently O(n) in number of modules) 
        // TODO relevant for dependency determination?
        java.util.Set<IEdge<IOwnableScope, ITerm, List<IOwnableScope>>> set = new HashSet<>();
        set.addAll(data.get(scope, label));
        for (IModule child : owner.getChildren()) {
            set.addAll(child.getScopeGraph().getTransitiveData(scope, label));
        }
        return set;
    }
    
    @Override
    public IOwnableScope createScope() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean addEdge(IOwnableScope sourceScope, ITerm label, IOwnableScope targetScope) {
        if (!scopes.contains(sourceScope) && !canExtend.contains(sourceScope)) {
            throw new IllegalArgumentException(
                    "addEdge directed to wrong scope graph: "
                    + "adding an edge to a scope that is not extendable by this scope graph. "
                    + "SG module: (" + this.owner + "), "
                    + "Scope: " + sourceScope + ", "
                    + "Edge: " + sourceScope.getName() + " -" + label + "-> " + targetScope.getName());
        }
        
        IEdge<IOwnableScope, ITerm, IOwnableScope> edge = new Edge<>(this.owner, sourceScope, label, targetScope);
        return edges.put(sourceScope, label, edge);
    }

    @Override
    public boolean addDatum(IOwnableScope scope, ITerm relation, Iterable<IOwnableScope> datum) {
        if (scope.getOwner() != this.owner) {
            System.out.println("Adding datum edge from unowned scope (" + scope.getOwner() + ") in " + this.owner);
        }
        if (!scopes.contains(scope) && !canExtend.contains(scope)) {
            throw new IllegalArgumentException(
                    "addDatum directed to wrong scope graph: "
                    + "adding a datum to a scope that is not extendable by this scope graph. "
                    + "SG module: (" + this.owner + "), "
                    + "Scope: " + scope + ", "
                    + "Datum: " + scope.getName() + " -" + relation + "-> " + datum.toString());
        }
        
        ArrayList<IOwnableScope> datumlist = new ArrayList<>();
        for (IOwnableScope v : datum) {
            datumlist.add(v);
        }
        
        IEdge<IOwnableScope, ITerm, List<IOwnableScope>> edge = new Edge<>(this.owner, scope, relation, datumlist);
        return data.put(scope, relation, edge);
    }
    
    @Override
    public IModule getOwner() {
        return this.owner;
    }
}
