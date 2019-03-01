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
import mb.statix.util.Capsules;

public class ModuleScopeGraph implements IMInternalScopeGraph<IOwnableTerm, ITerm, ITerm, ITerm>, IOwnable {
    protected final IModule owner;
    protected final Set.Immutable<ITerm> labels;
    protected final ITerm endOfPath;
    protected final Set.Immutable<ITerm> relations;
    /** Scopes from parent that you can extend. Used for checking if an edge addition is valid. */
    protected final Set.Immutable<IOwnableScope> canExtend;
    protected HashSet<IOwnableScope> scopes = new HashSet<>();
    protected HashSet<ModuleScopeGraph> children = new HashSet<>();
    
    protected IRelation3.Transient<IOwnableTerm, ITerm, IEdge<IOwnableTerm, ITerm, IOwnableTerm>> edges = HashTrieRelation3.Transient.of();
    protected IRelation3.Transient<IOwnableTerm, ITerm, IEdge<IOwnableTerm, ITerm, List<ITerm>>> data = HashTrieRelation3.Transient.of();

    private int scopeCounter;
    
    public ModuleScopeGraph(
            IModule owner,
            Iterable<ITerm> labels,
            ITerm endOfPath,
            Iterable<ITerm> relations,
            Set.Immutable<IOwnableScope> canExtend) {
        this(owner, Capsules.newSet(labels), endOfPath, Capsules.newSet(relations), canExtend);
    }
    
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
    public java.util.Set<IEdge<IOwnableTerm, ITerm, IOwnableTerm>> getEdges(IOwnableTerm scope, ITerm label) {
        if (scope.getOwner() == this.owner) {
            return getTransitiveEdges(scope, label);
        } else {
            return scope.getOwner().getScopeGraph().getEdges(scope, label);
        }
    }
    
    @Override
    public java.util.Set<IEdge<IOwnableTerm, ITerm, List<ITerm>>> getData(IOwnableTerm scope, ITerm label) {
        System.err.println("[getData] on " + owner.getId() + " scope: " + scope);
        if (scope.getOwner() == this.owner) {
            System.err.println("[getData] | is our scope, checking transitively...");
            return getTransitiveData(scope, label);
        } else {
            System.err.println("[getData] | redirecting to " + scope.getOwner().getId());
            return scope.getOwner().getScopeGraph().getData(scope, label);
        }
    }
    
    @Override
    public java.util.Set<IEdge<IOwnableTerm, ITerm, IOwnableTerm>> getTransitiveEdges(IOwnableTerm scope, ITerm label) {
        // OPTIMIZE Only query children if they can extend scope (currently O(n) in number of modules) 
        // TODO relevant for dependency determination?
        java.util.Set<IEdge<IOwnableTerm, ITerm, IOwnableTerm>> set = new HashSet<>();
        set.addAll(edges.get(scope, label));
        //TODO Use the canExtend set to only build this for our children.
        for (ModuleScopeGraph child : children) {
            set.addAll(child.getTransitiveEdges(scope, label));
        }
        return set;
    }

    @Override
    public java.util.Set<IEdge<IOwnableTerm, ITerm, List<ITerm>>> getTransitiveData(IOwnableTerm scope, ITerm label) {
        System.err.println("[traData] | | getting transitive data of " + owner.getId() + " scope: " + scope);
        // OPTIMIZE Only query children if they can extend scope (currently O(n) in number of modules) 
        // TODO relevant for dependency determination?
        java.util.Set<IEdge<IOwnableTerm, ITerm, List<ITerm>>> set = new HashSet<>();
        set.addAll(data.get(scope, label));
        for (ModuleScopeGraph child : children) {
            set.addAll(child.getTransitiveData(scope, label));
        }
        return set;
    }
    
    @Override
    public IOwnableTerm createScope(String base) {
        System.err.println("[" + owner.getId() + "] Creating scope " + base);
        int i = ++scopeCounter;
        
        String name = base.replaceAll("-", "_") + "-" + i;
        IOwnableScope scope = new OwnableScope(owner, name);
        scopes.add(scope);
        return scope;
    }

    @Override
    public boolean addEdge(IOwnableTerm sourceScope, ITerm label, IOwnableTerm targetScope) {
        if (!scopes.contains(sourceScope) && !canExtend.contains(sourceScope)) {
            throw new IllegalArgumentException(
                    "addEdge directed to wrong scope graph: "
                    + "adding an edge to a scope that is not extendable by this scope graph. "
                    + "SG module: (" + this.owner + "), "
                    + "Scope: " + sourceScope + ", "
                    + "Edge: " + sourceScope + " -" + label + "-> " + targetScope);
        }
        
        IEdge<IOwnableTerm, ITerm, IOwnableTerm> edge = new Edge<>(this.owner, sourceScope, label, targetScope);
        return edges.put(sourceScope, label, edge);
    }

    @Override
    public boolean addDatum(IOwnableTerm scope, ITerm relation, Iterable<ITerm> datum) {
        if (scope.getOwner() != this.owner) {
            System.out.println("Adding datum edge from unowned scope (" + scope.getOwner() + ") in " + this.owner);
        }
        if (!scopes.contains(scope) && !canExtend.contains(scope)) {
            throw new IllegalArgumentException(
                    "addDatum directed to wrong scope graph: "
                    + "adding a datum to a scope that is not extendable by this scope graph. "
                    + "SG module: (" + this.owner + "), "
                    + "Scope: " + scope + ", "
                    + "Datum: " + scope + " -" + relation + "-> " + datum.toString());
        }
        
        ArrayList<ITerm> datumlist = new ArrayList<>();
        for (ITerm v : datum) {
            datumlist.add(v);
        }
        
        IEdge<IOwnableTerm, ITerm, List<ITerm>> edge = new Edge<>(this.owner, scope, relation, datumlist);
        return data.put(scope, relation, edge);
    }
    
    @Override
    public IModule getOwner() {
        return this.owner;
    }
    
    @Override
    public ModuleScopeGraph createChild(IModule module, Set.Immutable<IOwnableScope> canExtend) {
        ModuleScopeGraph child = new ModuleScopeGraph(module, labels, endOfPath, relations, canExtend);
        children.add(child);
        return child;
    }
}
