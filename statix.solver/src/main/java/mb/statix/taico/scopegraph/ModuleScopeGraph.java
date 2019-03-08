package mb.statix.taico.scopegraph;

import java.util.ArrayList;
import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import io.usethesource.capsule.Set.Immutable;
import mb.nabl2.terms.ITerm;
import mb.nabl2.util.collections.HashTrieRelation3;
import mb.nabl2.util.collections.IRelation3;
import mb.statix.taico.module.IModule;
import mb.statix.taico.util.IOwnable;
import mb.statix.util.Capsules;

public class ModuleScopeGraph implements IMInternalScopeGraph<IOwnableTerm, ITerm, ITerm, ITerm>, IOwnable {
    private static AtomicInteger idCounter = new AtomicInteger();
    //Constants for this module
    protected final IModule owner;
    protected final Immutable<? extends ITerm> labels;
    protected final ITerm endOfPath;
    protected final Immutable<? extends ITerm> relations;
    /** Scopes from parent that you can extend. Used for checking if an edge addition is valid. */
    protected final Immutable<? extends IOwnableScope> canExtend;
    
    //Scope graph graph
    protected final ModuleScopeGraph parent;
    protected final HashSet<ModuleScopeGraph> children = new HashSet<>();
    
    protected final HashSet<IOwnableScope> scopes = new HashSet<>();
    protected final IRelation3.Transient<IOwnableTerm, ITerm, IEdge<IOwnableTerm, ITerm, IOwnableTerm>> edges = HashTrieRelation3.Transient.of();
    protected final IRelation3.Transient<IOwnableTerm, ITerm, IEdge<IOwnableTerm, ITerm, List<ITerm>>> data = HashTrieRelation3.Transient.of();

    private int scopeCounter;
    private int id;
    private int copyId;
    private ModuleScopeGraph original;
    
    public ModuleScopeGraph(
            ModuleScopeGraph parent,
            IModule owner,
            Iterable<? extends ITerm> labels,
            ITerm endOfPath,
            Iterable<? extends ITerm> relations,
            Iterable<? extends IOwnableScope> canExtend) {
        this(idCounter.getAndIncrement(), parent, owner, labels, endOfPath, relations, canExtend);
    }
    
    private ModuleScopeGraph(
            int id,
            ModuleScopeGraph parent,
            IModule owner,
            Iterable<? extends ITerm> labels,
            ITerm endOfPath,
            Iterable<? extends ITerm> relations,
            Iterable<? extends IOwnableScope> canExtend) {
        this.id = id;
        this.parent = parent;
        this.owner = owner;
        this.labels = labels instanceof Immutable ? (Immutable<? extends ITerm>) labels : Capsules.newSet(labels);
        this.endOfPath = endOfPath;
        assert this.labels.contains(endOfPath);
        this.relations = relations instanceof Immutable ? (Immutable<? extends ITerm>) relations : Capsules.newSet(relations);
        this.canExtend = canExtend instanceof Immutable ? (Immutable<? extends IOwnableScope>) canExtend : Capsules.newSet(canExtend);
    }
    
    @Override
    public IModule getOwner() {
        return this.owner;
    }
    
    @Override
    public ITerm getEndOfPath() {
        return this.endOfPath;
    }
    
    @Override
    public Immutable<? extends ITerm> getLabels() {
        return labels;
    }

    @Override
    public Immutable<? extends ITerm> getRelations() {
        return relations;
    }
    
    @Override
    public IRelation3<IOwnableTerm, ITerm, IEdge<IOwnableTerm, ITerm, List<ITerm>>> getData() {
        return data;
    }
    
    @Override
    public IRelation3<IOwnableTerm, ITerm, IEdge<IOwnableTerm, ITerm, IOwnableTerm>> getEdges() {
        return edges;
    }
    
    public Set<IOwnableScope> getScopes() {
        return scopes;
    }
    
    @Override
    public Immutable<? extends IOwnableScope> getExtensibleScopes() {
        return canExtend;
    }

    @Override
    public Set<IEdge<IOwnableTerm, ITerm, IOwnableTerm>> getEdges(IOwnableTerm scope, ITerm label) {
        if (scope.getOwner() == this.owner) {
            return getTransitiveEdges(scope, label);
        } else {
            return scope.getOwner().getScopeGraph().getEdges(scope, label);
        }
    }
    
    @Override
    public Set<IEdge<IOwnableTerm, ITerm, List<ITerm>>> getData(IOwnableTerm scope, ITerm label) {
        System.err.println("[getData] on " + owner.getId() + " scope: " + scope);
        if (scope.getOwner() == this.owner) {
            System.err.println("[getData] | is our scope, checking transitively...");
            Set<IEdge<IOwnableTerm, ITerm, List<ITerm>>> data = getTransitiveData(scope, label);
            System.err.println("[getData] | found " + data.size() + " datums: " + data);
            return data;
        } else {
            System.err.println("[getData] | redirecting to " + scope.getOwner().getId());
            Set<IEdge<IOwnableTerm, ITerm, List<ITerm>>> data = scope.getOwner().getScopeGraph().getData(scope, label);
            System.err.println("[getData] | result from redirect: " + data.size() + " datums: " + data);
            return data;
        }
    }
    
    @Override
    public Set<IEdge<IOwnableTerm, ITerm, IOwnableTerm>> getTransitiveEdges(IOwnableTerm scope, ITerm label) {
        // OPTIMIZE Only query children if they can extend scope (currently O(n) in number of modules) 
        // TODO relevant for dependency determination?
        Set<IEdge<IOwnableTerm, ITerm, IOwnableTerm>> set = new HashSet<>();
        set.addAll(edges.get(scope, label));
        //TODO Use the canExtend set to only build this for our children.
        for (ModuleScopeGraph child : children) {
            set.addAll(child.getTransitiveEdges(scope, label));
        }
        return set;
    }

    @Override
    public Set<IEdge<IOwnableTerm, ITerm, List<ITerm>>> getTransitiveData(IOwnableTerm scope, ITerm label) {
        System.err.println("[traData] | | getting transitive data of " + owner.getId() + " scope: " + scope);
        // OPTIMIZE Only query children if they can extend scope (currently O(n) in number of modules) 
        // TODO relevant for dependency determination?
        Set<IEdge<IOwnableTerm, ITerm, List<ITerm>>> set = new HashSet<>();
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
    public void revokeScope(IOwnableTerm scope) {
        System.err.println("[" + owner.getId() + "] Deleting scope " + scope);
        if (!scopes.remove(scope)) {
            throw new IllegalStateException("Scope is already removed!");
        }
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
    
    public ModuleScopeGraph getParent() {
        return parent;
    }
    
    @Override
    public ModuleScopeGraph createChild(IModule module, Iterable<IOwnableScope> canExtend) {
        ModuleScopeGraph child = new ModuleScopeGraph(this, module, labels, endOfPath, relations, canExtend);
        children.add(child);
        return child;
    }
    
    @Override
    public Collection<ModuleScopeGraph> getChildren() {
        return children;
    }
    
    @Override
    public synchronized ModuleScopeGraph deepCopy(IMInternalScopeGraph<IOwnableTerm, ITerm, ITerm, ITerm> parent) {
        if (parent != null && !(parent instanceof ModuleScopeGraph)) throw new IllegalArgumentException("the parent must be a module scope graph");
        ModuleScopeGraph msg = new ModuleScopeGraph(id, (ModuleScopeGraph) parent, owner, labels, endOfPath, relations, canExtend);
        msg.original = this;
        msg.copyId = this.copyId + 1;
        msg.scopeCounter = this.scopeCounter;
        msg.scopes.addAll(this.scopes);
        msg.edges.putAll(this.edges);
        msg.data.putAll(this.data);
        for (ModuleScopeGraph child : this.children) {
            msg.children.add(child.deepCopy(msg));
        }
        
        return msg;
    }
    
    @Override
    public synchronized void updateToCopy(IMInternalScopeGraph<IOwnableTerm, ITerm, ITerm, ITerm> copyI, boolean checkConcurrency) {
        if (this == copyI) return;
        if (!(copyI instanceof ModuleScopeGraph)) throw new IllegalArgumentException("The copy must be a module scope graph");
        ModuleScopeGraph copy = (ModuleScopeGraph) copyI;
        
        System.err.println("Updating " + this + " with " + copy);
        
        if (copy.id != this.id) {
            throw new IllegalArgumentException("The given scope graph is not a copy of this scope graph!");
        }
        
        if (checkConcurrency) {
            if (this.scopeCounter > copy.scopeCounter) {
                throw new ConcurrentModificationException("Concurrent modification detected in the scope graph when updating to a copy (scope counter)");
            } else if (!copy.scopes.containsAll(this.scopes)) {
                throw new ConcurrentModificationException("Concurrent modification detected in the scope graph when updating to a copy (scopes)");
            } else if (!containsAll(copy.edges, this.edges)) {
                throw new ConcurrentModificationException("Concurrent modification detected in the scope graph when updating to a copy (edges)");
            } else if (!containsAll(copy.data, this.data)) {
                throw new ConcurrentModificationException("Concurrent modification detected in the scope graph when updating to a copy (data)");
            } else if (!copy.children.containsAll(this.children)) {
                throw new ConcurrentModificationException("Concurrent modification detected in the scope graph when updating to a copy (children)");
            }
        }
        
        this.scopeCounter = copy.scopeCounter;
        this.scopes.addAll(copy.scopes);
        this.edges.putAll(copy.edges);
        this.data.putAll(copy.data);
        for (ModuleScopeGraph copyChild : copy.children) {
            if (copyChild.original != null) {
                copyChild.original.updateToCopy(copyChild, checkConcurrency);
            } else {
                //Create a copy, make it the original
                ModuleScopeGraph copyChildCopy = copyChild.deepCopy(this);
                copyChildCopy.original = copyChild.original; //Mark the new child as the original
                copyChild.original = copyChildCopy; //Create a link from the copy to the "original"
                this.children.add(copyChildCopy);
            }
        }
    }
    
    @Override
    public int hashCode() {
        return this.id;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (!(obj instanceof ModuleScopeGraph)) return false;
        ModuleScopeGraph other = (ModuleScopeGraph) obj;
        return this.id == other.id;
    }
    
    @Override
    public String toString() {
        return "SG<@" + owner.getId() + ", " + id + "_" + copyId + ">";
    }
    
    private static <A, B, C> boolean containsAll(IRelation3<A, B, C> haystack, IRelation3<A, B, C> needle) {
        //If there is ANY element that is not contained (anyMatch(c -> !c) == true), then the result should be false.
        return !needle.stream(haystack::contains).anyMatch(c -> !c);
    }
}
