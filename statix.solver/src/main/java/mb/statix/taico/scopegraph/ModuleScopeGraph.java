package mb.statix.taico.scopegraph;

import static mb.statix.taico.util.TOverrides.*;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;

import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.unification.IUnifier;
import mb.nabl2.util.collections.HashTrieRelation3;
import mb.nabl2.util.collections.IRelation3;
import mb.statix.scopegraph.terms.Scope;
import mb.statix.spec.Spec;
import mb.statix.taico.dot.DotPrinter;
import mb.statix.taico.module.IModule;
import mb.statix.taico.module.ModulePaths;
import mb.statix.taico.solver.Context;
import mb.statix.taico.util.IOwnable;
import mb.statix.taico.util.Scopes;
import mb.statix.taico.util.TOverrides;
import mb.statix.taico.util.TPrettyPrinter;
import mb.statix.util.Capsules;

public class ModuleScopeGraph implements IMInternalScopeGraph<Scope, ITerm, ITerm>, IOwnable {
    private static final long serialVersionUID = 1L;
    
    //Identity
    private final IModule owner;
    
    //Labels
    private final io.usethesource.capsule.Set.Immutable<ITerm> edgeLabels;
    private final io.usethesource.capsule.Set.Immutable<ITerm> dataLabels;
    private final ITerm noDataLabel;
    
    //Scopes, edges and data
    private io.usethesource.capsule.Set.Immutable<? extends Scope> canExtend;
    private List<? extends Scope> parentScopes;
    private final Set<Scope> scopes = synchronizedSet(new HashSet<>());
    private transient IRelation3.Transient<Scope, ITerm, Scope> edges = HashTrieRelation3.Transient.of();
    private transient IRelation3.Transient<Scope, ITerm, ITerm> data = HashTrieRelation3.Transient.of();
    
    //Child graphs
    private final HashSet<String> children = new HashSet<>();
    
    //Locking
    private transient ReadWriteLock lock;
    private transient boolean useLock;
    
    public ModuleScopeGraph(
            IModule owner,
            Iterable<ITerm> edgeLabels,
            Iterable<ITerm> dataLabels,
            ITerm noDataLabel,
            List<? extends Scope> canExtend) {
        this.owner = owner;
        this.edgeLabels = edgeLabels instanceof io.usethesource.capsule.Set.Immutable ? (io.usethesource.capsule.Set.Immutable<ITerm>) edgeLabels : Capsules.newSet(edgeLabels);
        this.dataLabels = dataLabels instanceof io.usethesource.capsule.Set.Immutable ? (io.usethesource.capsule.Set.Immutable<ITerm>) dataLabels : Capsules.newSet(dataLabels);
        this.noDataLabel = noDataLabel;
        this.parentScopes = canExtend;
        this.canExtend = Capsules.newSet(canExtend);
        this.useLock = useLock();
        if (this.useLock) lock = TOverrides.readWriteLock();
    }
    
    @Override
    public IModule getOwner() {
        return owner;
    }
    
    @Override
    public void clear() {
        edges = HashTrieRelation3.Transient.of();
        data = HashTrieRelation3.Transient.of();
        scopes.clear();
        children.clear();
    }
    
    // --------------------------------------------------------------------------------------------
    // Labels
    // --------------------------------------------------------------------------------------------
    
    @Override
    public ITerm getNoDataLabel() {
        return noDataLabel;
    }
    
    @Override
    public io.usethesource.capsule.Set.Immutable<ITerm> getEdgeLabels() {
        return edgeLabels;
    }

    @Override
    public io.usethesource.capsule.Set.Immutable<ITerm> getDataLabels() {
        return dataLabels;
    }
    
    // --------------------------------------------------------------------------------------------
    // Edges and data
    // --------------------------------------------------------------------------------------------

    @Deprecated @Override
    public IRelation3<Scope, ITerm, Scope> getEdges() {
        throw new UnsupportedOperationException("Directly getting edges!");
    }
    
    @Deprecated @Override
    public IRelation3<Scope, ITerm, ITerm> getData() {
        throw new UnsupportedOperationException("Directly getting data!");
    }
    
    @Override
    public IRelation3<Scope, ITerm, ITerm> getOwnData() {
        return data;
    }
    
    @Override
    public IRelation3<Scope, ITerm, Scope> getOwnEdges() {
        return edges;
    }
    
    @Override
    public Set<Scope> getEdges(Scope scope, ITerm label) {
        System.err.println("WARNING: Unchecked access to edges: " + scope + " -" + label + "->");
        if (owner.getId().equals(scope.getResource())) {
            return getTransitiveEdges(scope, label);
        } else {
            //TODO IMPORTANT Should the requester be the owner of this scope graph? Or should it be the one asking this query?
            return Scopes.getOwner(scope, owner).getScopeGraph().getTransitiveEdges(scope, label);
        }
    }
    
    @Override
    public Set<Scope> getEdges(Scope scope, ITerm label, String requester) {
        if (owner.getId().equals(scope.getResource())) {
            return getTransitiveEdges(scope, label);
        } else {
            return Scopes.getOwner(scope, requester).getScopeGraph().getTransitiveEdges(scope, label);
        }
    }
    
    @Override
    public Set<ITerm> getData(Scope scope, ITerm relation) {
        System.err.println("WARNING: Unchecked access to data: " + scope + " -" + relation + "->");
        if (owner.getId().equals(scope.getResource())) {
            return getTransitiveData(scope, relation);
        } else {
            //TODO IMPORTANT Should the requester be the owner of this scope graph? Or should it be the one asking this query?
            return Scopes.getOwner(scope, owner).getScopeGraph().getTransitiveData(scope, relation);
        }
    }
    
    @Override
    public Set<ITerm> getData(Scope scope, ITerm relation, String requester) {
        if (owner.getId().equals(scope.getResource())) {
            return getTransitiveData(scope, relation);
        } else {
            return Scopes.getOwner(scope, requester).getScopeGraph().getTransitiveData(scope, relation);
        }
    }
    
    @Override
    public void getTransitiveEdges(Scope scope, ITerm label, Collection<Scope> edges) {
        if (useLock) {
            getReadLock().lock();
            try {
                edges.addAll(getOwnEdges().get(scope, label));
                
                //TODO OPTIMIZATION We might be able to do a better check than just the scopes that are passed based on the spec. 
                for (IMInternalScopeGraph<Scope, ITerm, ITerm> child : getChildren()) {
                    if (child.getExtensibleScopes().contains(scope)) {
                        child.getTransitiveEdges(scope, label, edges);
                    }
                }
            } finally {
                getReadLock().unlock();
            }
        } else {
            final IRelation3<Scope, ITerm, Scope> ownEdges = getOwnEdges();
            synchronized (ownEdges) {
                edges.addAll(ownEdges.get(scope, label));
            }
            
            synchronized (children) {
                //TODO OPTIMIZATION We might be able to do a better check than just the scopes that are passed based on the spec. 
                for (IMInternalScopeGraph<Scope, ITerm, ITerm> child : getChildren()) {
                    if (child.getExtensibleScopes().contains(scope)) {
                        child.getTransitiveEdges(scope, label, edges);
                    }
                }
            }
        }
    }
    
    @Override
    public void getTransitiveData(Scope scope, ITerm label, Collection<ITerm> data) {
        if (useLock) {
            getReadLock().lock();
            try {
                data.addAll(getOwnData().get(scope, label));
                
                //TODO OPTIMIZATION We might be able to do a better check than just the scopes that are passed based on the spec. 
                for (IMInternalScopeGraph<Scope, ITerm, ITerm> child : getChildren()) {
                    if (child.getExtensibleScopes().contains(scope)) {
                        child.getTransitiveData(scope, label, data);
                    }
                }
            } finally {
                getReadLock().unlock();
            }
        } else {
            final IRelation3<Scope, ITerm, ITerm> ownData = getOwnData();
            synchronized (ownData) {
                data.addAll(ownData.get(scope, label));
            }
            
            synchronized (children) {
                //TODO OPTIMIZATION We might be able to do a better check than just the scopes that are passed based on the spec. 
                for (IMInternalScopeGraph<Scope, ITerm, ITerm> child : getChildren()) {
                    if (child.getExtensibleScopes().contains(scope)) {
                        child.getTransitiveData(scope, label, data);
                    }
                }
            }
        }
    }
    
    @Override
    public boolean addEdge(Scope sourceScope, ITerm label, Scope targetScope) {
        if (!getScopes().contains(sourceScope) && !getExtensibleScopes().contains(sourceScope)) {
            throw new IllegalArgumentException(
                    "addEdge directed to wrong scope graph: "
                    + "adding an edge to a scope that is not extendable by this scope graph. "
                    + "SG module: (" + this.owner + "), "
                    + "Scope: " + sourceScope + ", "
                    + "Edge: " + sourceScope + " -" + label + "-> " + targetScope);
        }
        
        if (useLock) {
            getWriteLock().lock();
            try {
                return edges.put(sourceScope, label, targetScope);
            } finally {
                getWriteLock().unlock();
            }
        } else {
            synchronized (edges) {
                return edges.put(sourceScope, label, targetScope);
            }
        }
    }

    @Override
    public boolean addDatum(Scope scope, ITerm relation, ITerm datum) {
        if (!getScopes().contains(scope) && !getExtensibleScopes().contains(scope)) {
            throw new IllegalArgumentException(
                    "addDatum directed to wrong scope graph: "
                    + "adding a datum to a scope that is not extendable by this scope graph. "
                    + "SG module: (" + this.owner + "), "
                    + "Scope: " + scope + ", "
                    + "Datum: " + scope + " -" + relation + "-> " + datum.toString());
        }
        
        if (useLock) {
            getWriteLock().lock();
            try {
                return data.put(scope, relation, datum);
            } finally {
                getWriteLock().unlock();
            }
        } else {
            synchronized (data) {
                return data.put(scope, relation, datum);
            }
        }
    }
    
    // --------------------------------------------------------------------------------------------
    // Scopes
    // --------------------------------------------------------------------------------------------
    
    @Deprecated @Override
    public io.usethesource.capsule.Set<Scope> getAllScopes() {
        throw new UnsupportedOperationException("Directly getting all scopes!");
    }
    
    @Override
    public Set<Scope> getScopes() {
        return scopes;
    }
    
    @Override
    public io.usethesource.capsule.Set.Immutable<? extends Scope> getExtensibleScopes() {
        return canExtend;
    }
    
    @Override
    public List<? extends Scope> getParentScopes() {
        return parentScopes;
    }
    
    @Override
    public Scope createScopeWithIdentity(String identity) {
        Scope scope = Scope.of(owner.getId(), identity);
        scopes.add(scope);
        return scope;
    }

    // --------------------------------------------------------------------------------------------
    // Children
    // --------------------------------------------------------------------------------------------
    
    @Override
    public boolean hasChild(String childId) {
        return children.contains(childId);
    }
    
    @Override
    public ModuleScopeGraph createChild(IModule module, List<Scope> canExtend) {
        ModuleScopeGraph child = new ModuleScopeGraph(module, edgeLabels, dataLabels, noDataLabel, canExtend);
        return child;
    }
    
    @Override
    public ModuleScopeGraph addChild(IModule child) {
        //TODO Unsafe cast
        ModuleScopeGraph childSg = (ModuleScopeGraph) child.getScopeGraph();
        
        if (useLock) {
            getWriteLock().lock();
            try {
                children.add(child.getId());
            } finally {
                getWriteLock().unlock();
            }
        } else {
            synchronized (children) {
                children.add(child.getId());
            }
        }
        
        return childSg;
    }
    
    @Override
    public boolean removeChild(IModule child) {
        if (useLock) {
            getWriteLock().lock();
            try {
                return children.remove(child.getId());
            } finally {
                getWriteLock().unlock();
            }
        } else {
            synchronized (children) {
                return children.remove(child.getId());
            }
        }
    }
    
    @Override
    public Iterable<? extends IMInternalScopeGraph<Scope, ITerm, ITerm>> getChildren() {
        return children.stream().map(Context.context()::getScopeGraph)::iterator;
    }
    
    @Override
    public Set<String> getChildIds() {
        return children;
    }
    
    @Override
    public void purgeChildren() {
        for (IMInternalScopeGraph<Scope, ITerm, ITerm> childSg : getChildren()) {
            childSg.purgeChildren();
        }
        
        if (useLock) {
            getWriteLock().lock();
            try {
                children.clear();
            } finally {
                getWriteLock().unlock();
            }
        } else {
            synchronized (children) {
                children.clear();
            }
        }
    }
    
    // --------------------------------------------------------------------------------------------
    // Other graphs
    // --------------------------------------------------------------------------------------------
    
    @Deprecated
    @Override
    public synchronized void substitute(List<? extends Scope> newScopes) {
        if (parentScopes.equals(newScopes)) {
            System.err.println("Skipping substitution of scopes, no substitution necessary.");
            return;
        }
        //Sometimes the order of constraints changes the scope numbers, so substitution is necessary.
        List<? extends Scope> oldScopes = parentScopes;
        parentScopes = newScopes;
        canExtend = Capsules.newSet(newScopes);
        
        //scopes should be stored as strings in the sets to avoid substitution
        IRelation3.Transient<Scope, ITerm, Scope> newEdges = HashTrieRelation3.Transient.of();
        IRelation3.Transient<Scope, ITerm, ITerm> newData = HashTrieRelation3.Transient.of();
        for (int i = 0; i < oldScopes.size(); i++) {
            Scope oldScope = oldScopes.get(i);
            Scope newScope = parentScopes.get(i);
            for (Entry<ITerm, Scope> e : getOwnEdges().get(oldScope)) {
                newEdges.put(newScope, e.getKey(), e.getValue());
            }
            for (Entry<ITerm, ITerm> e : getOwnData().get(oldScope)) {
                newData.put(newScope, e.getKey(), e.getValue());
            }
        }
        
        if (useLock) {
            getWriteLock().lock();
            edges = newEdges;
            data = newData;
            getWriteLock().unlock();
        } else {
            edges = newEdges;
            data = newData;
        }
        
    }
    
    @Override
    public ModuleScopeGraph externalGraph() {
        ModuleScopeGraph msg = new ModuleScopeGraph(owner, edgeLabels, dataLabels, noDataLabel, parentScopes);
        
        IUnifier.Immutable unifier = owner.getCurrentState().unifier();
        
        Queue<Scope> scopes = new LinkedList<>(parentScopes);
        scopes.addAll(this.scopes);
        final IRelation3<Scope, ITerm, Scope> ownEdges = getOwnEdges();
        final IRelation3<Scope, ITerm, ITerm> ownData = getOwnData();
        if (useLock) {
            getReadLock().lock();
            try {
                while (!scopes.isEmpty()) {
                    Scope scope = scopes.poll();
                    if (owner.getId().equals(scope.getResource())) {
                        msg.scopes.add(scope);
                    }
                    
                    for (Entry<ITerm, Scope> e : ownEdges.get(scope)) {
                        msg.addEdge(scope, e.getKey(), e.getValue());
                    }
                    
                    for (Entry<ITerm, ITerm> e : ownData.get(scope)) {
                        ITerm data = e.getValue();
                        msg.addDatum(scope, e.getKey(), data);
                        for (Scope s : Scopes.getScopesInTerm(data, unifier)) {
                            if (!owner.getId().equals(scope.getResource())) continue;
                            if (!msg.scopes.contains(s)) scopes.add(s);
                        }
                    }
                }
            } finally {
                getReadLock().unlock();
            }
        } else {
            while (!scopes.isEmpty()) {
                Scope scope = scopes.poll();
                if (owner.getId().equals(scope.getResource())) {
                    msg.scopes.add(scope);
                }
                
                synchronized (ownEdges) {
                    for (Entry<ITerm, Scope> e : ownEdges.get(scope)) {
                        msg.addEdge(scope, e.getKey(), e.getValue());
                    }
                }
                
                synchronized (ownData) {
                    for (Entry<ITerm, ITerm> e : ownData.get(scope)) {
                        ITerm data = e.getValue();
                        msg.addDatum(scope, e.getKey(), data);
                        for (Scope s : Scopes.getScopesInTerm(data, unifier)) {
                            if (!owner.getId().equals(scope.getResource())) continue;
                            scopes.add(s);
                        }
                    }
                }
            }
        }
        
        msg.children.addAll(this.children);
        
        return msg;
    }
    
    @Override
    public DelegatingModuleScopeGraph delegatingGraph(boolean clearScopes) {
        return new DelegatingModuleScopeGraph(this, clearScopes);
    }
    
    // --------------------------------------------------------------------------------------------
    // Locks
    // --------------------------------------------------------------------------------------------
    
    /**
     * @return
     *      true if locks should be used, false if synchronized should be used
     */
    private boolean useLock() {
        if (!CONCURRENT || SYNC_SCOPEGRAPHS == 0) return true;
        
        return SYNC_SCOPEGRAPHS - ModulePaths.pathLength(owner.getId()) > 0;
    }
    
    @Override
    public Lock getReadLock() {
        return lock.readLock();
    }
    
    @Override
    public Lock getWriteLock() {
        return lock.writeLock();
    }
    
    // --------------------------------------------------------------------------------------------
    // Copy
    // --------------------------------------------------------------------------------------------
    
    private ModuleScopeGraph(ModuleScopeGraph original) {
        this.owner = original.owner;
        this.canExtend = original.canExtend;
        this.children.addAll(original.children);
        
        IRelation3.Immutable<Scope, ITerm, ITerm> nData = original.data.freeze();
        original.data = nData.melt();
        this.data = nData.melt();
        
        IRelation3.Immutable<Scope, ITerm, Scope> nEdges = original.edges.freeze();
        original.edges = nEdges.melt();
        this.edges = nEdges.melt();
        
        this.dataLabels = original.dataLabels;
        this.edgeLabels = original.edgeLabels;
        this.noDataLabel = original.noDataLabel;
        this.parentScopes = original.parentScopes;
        this.scopes.addAll(original.scopes);
        this.useLock = useLock();
        if (useLock) this.lock = TOverrides.readWriteLock();
    }
    
    @Override
    public ModuleScopeGraph copy() {
        return new ModuleScopeGraph(this);
    }
    
    // --------------------------------------------------------------------------------------------
    // Object methods
    // --------------------------------------------------------------------------------------------
    
    @Override
    public String toString() {
        return "SG<" +
                "owner=" + TPrettyPrinter.printModule(owner) +
                ", scopes=" + TPrettyPrinter.prettyPrint(scopes) +
                ", edges=" + TPrettyPrinter.prettyPrint(edges) +
                ", data=" + TPrettyPrinter.prettyPrint(data) +
                ">";
    }
    
    // --------------------------------------------------------------------------------------------
    // Serialization
    // --------------------------------------------------------------------------------------------
    
    @SuppressWarnings("unchecked")
    private void readObject(java.io.ObjectInputStream stream) throws IOException, ClassNotFoundException {
        stream.defaultReadObject();
        
        //Transient HashTrieRelation3 is not serializable, but the immutable variant is, so we read the frozen variant and melt it
        this.edges = ((HashTrieRelation3.Immutable<Scope, ITerm, Scope>) stream.readObject()).melt();
        this.data = ((HashTrieRelation3.Immutable<Scope, ITerm, ITerm>) stream.readObject()).melt();
        
        //Recreate the lock
        this.useLock = useLock();
        if (useLock) this.lock = TOverrides.readWriteLock();
    }
    
    private void writeObject(java.io.ObjectOutputStream stream) throws IOException {
        stream.defaultWriteObject();
        
        IRelation3.Immutable<Scope, ITerm, Scope> frozenEdges = edges.freeze();
        IRelation3.Immutable<Scope, ITerm, ITerm> frozenData = data.freeze();
        //Transient HashTrieRelation3 is not serializable, but the immutable variant is, so we need to write the frozen version
        stream.writeObject(frozenEdges);
        stream.writeObject(frozenData);
        
        this.edges = frozenEdges.melt();
        this.data = frozenData.melt();
    }
    
    /**
     * Prints this scope graph as a dot file.
     * 
     * @param includeChildren
     *      if children should be included
     * 
     * @return
     *      the string representing the dot file
     */
    public String printDot(boolean includeChildren) {
        return new DotPrinter(this, true).printDot();
    }
    
    /**
     * @param file
     *      the file to write to
     * @param includeChildren
     *      if child scope graphs should be included
     * 
     * @throws IOException
     *      If writing to the given file encounters an IOException.
     */
    public void writeDot(File file, boolean includeChildren) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
            writer.write(printDot(includeChildren));
        }
    }
    
    // --------------------------------------------------------------------------------------------
    // Static creation
    // --------------------------------------------------------------------------------------------
    
    /**
     * Creates an empty scope graph for the given module based on the spec (from the context).
     * The returned scope graph will have the given module as owner, but has no parent scopes and
     * cannot extend any foreign scopes.
     * 
     * @param module
     *      the module
     * 
     * @return
     *      an empty scope graph for the given module
     */
    public static ModuleScopeGraph empty(IModule module) {
        Spec spec = Context.context().getSpec();
        return new ModuleScopeGraph(
                module,
                spec.edgeLabels(),
                spec.relationLabels(),
                spec.noRelationLabel(),
                Collections.emptyList());
    }
    
    /**
     * Creates an empty scope graph with the same details as the given graph. More specifically,
     * the returned graph will have the same owner, edge labels, data labels, no data label and
     * parent scopes as the given graph, but all other information is removed.
     * 
     * @param graph
     *      the graph
     * 
     * @return
     *      an empty scope graph based on the given scope graph
     */
    public static ModuleScopeGraph empty(IMInternalScopeGraph<Scope, ITerm, ITerm> graph) {
        return new ModuleScopeGraph(
                graph.getOwner(),
                graph.getEdgeLabels(),
                graph.getDataLabels(),
                graph.getNoDataLabel(),
                graph.getParentScopes());
    }
}
