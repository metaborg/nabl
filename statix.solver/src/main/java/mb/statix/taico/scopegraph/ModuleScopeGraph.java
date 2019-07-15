package mb.statix.taico.scopegraph;

import static mb.statix.taico.util.TOverrides.*;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;

import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.ITermVar;
import mb.nabl2.terms.unification.IUnifier;
import mb.nabl2.util.collections.HashTrieRelation3;
import mb.nabl2.util.collections.IRelation3;
import mb.statix.scopegraph.terms.Scope;
import mb.statix.taico.dot.DotPrinter;
import mb.statix.taico.module.IModule;
import mb.statix.taico.module.ModulePaths;
import mb.statix.taico.solver.SolverContext;
import mb.statix.taico.util.IOwnable;
import mb.statix.taico.util.Scopes;
import mb.statix.taico.util.TOverrides;
import mb.statix.util.Capsules;

public class ModuleScopeGraph implements IMInternalScopeGraph<Scope, ITerm, ITerm>, IOwnable {
    private static final long serialVersionUID = 1L;
    
    private static AtomicInteger idCounter = new AtomicInteger();
    //Constants for this module
    private final IModule owner;
    private final io.usethesource.capsule.Set.Immutable<ITerm> edgeLabels;
    private final io.usethesource.capsule.Set.Immutable<ITerm> dataLabels;
    private final ITerm noDataLabel;
    
    /** Scopes from parent that you can extend. Used for checking if an edge addition is valid. */
    private final io.usethesource.capsule.Set.Immutable<? extends Scope> canExtend;
    private List<? extends Scope> parentScopes;
    
    //Scope graph graph
    private final HashSet<String> children = new HashSet<>();
    
    private final Set<Scope> scopes = synchronizedSet(new HashSet<>());
    private transient IRelation3.Transient<Scope, ITerm, Scope> edges = HashTrieRelation3.Transient.of();
    private transient IRelation3.Transient<Scope, ITerm, ITerm> data = HashTrieRelation3.Transient.of();

    protected int scopeCounter;
    protected int id;
    private int copyId;
    
    protected transient ReadWriteLock lock;
    protected transient boolean useLock;
    
    protected volatile transient int currentModification;
    
    public ModuleScopeGraph(
            IModule owner,
            Iterable<ITerm> edgeLabels,
            Iterable<ITerm> dataLabels,
            ITerm noDataLabel,
            List<? extends Scope> canExtend) {
        this(idCounter.getAndIncrement(), owner, edgeLabels, dataLabels, noDataLabel, canExtend);
    }
    
    protected ModuleScopeGraph(
            int id,
            IModule owner,
            Iterable<ITerm> edgeLabels,
            Iterable<ITerm> dataLabels,
            ITerm noDataLabel,
            List<? extends Scope> canExtend) {
        this.id = id;
        this.owner = owner;
        this.edgeLabels = edgeLabels instanceof io.usethesource.capsule.Set.Immutable ? (io.usethesource.capsule.Set.Immutable<ITerm>) edgeLabels : Capsules.newSet(edgeLabels);
        this.dataLabels = dataLabels instanceof io.usethesource.capsule.Set.Immutable ? (io.usethesource.capsule.Set.Immutable<ITerm>) dataLabels : Capsules.newSet(dataLabels);
        this.noDataLabel = noDataLabel;
        this.parentScopes = canExtend;
        this.canExtend = Capsules.newSet(canExtend);
        this.useLock = useLock();
        if (this.useLock) lock = TOverrides.readWriteLock();
    }
    
    /**
     * @return
     *      true if locks should be used, false if synchronized should be used
     */
    private boolean useLock() {
        if (!CONCURRENT || SYNC_SCOPEGRAPHS == 0) return true;
        
        return SYNC_SCOPEGRAPHS - ModulePaths.pathLength(owner.getId()) > 0;
    }
    
    @Override
    public IModule getOwner() {
        return owner;
    }
    
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
    
    @Override
    public io.usethesource.capsule.Set<Scope> getAllScopes() {
        throw new UnsupportedOperationException("Directly getting all scopes!");
    }
    
    @Override
    public IRelation3<Scope, ITerm, Scope> getEdges() {
        throw new UnsupportedOperationException("Directly getting edges!");
    }
    
    @Override
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
    public Set<Scope> getEdges(Scope scope, ITerm label) {
        if (owner.getId().equals(scope.getResource())) {
            return getTransitiveEdges(scope, label);
        } else {
            //TODO IMPORTANT Should the requester be the owner of this scope graph? Or should it be the one asking this query?
            return Scopes.getOwner(scope, owner).getScopeGraph().getTransitiveEdges(scope, label);
        }
    }
    
    @Override
    public Set<ITerm> getData(Scope scope, ITerm label) {
        if (owner.getId().equals(scope.getResource())) {
            return getTransitiveData(scope, label);
        } else {
            //TODO IMPORTANT Should the requester be the owner of this scope graph? Or should it be the one asking this query?
            return Scopes.getOwner(scope, owner).getScopeGraph().getTransitiveData(scope, label);
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
    public Scope createScope(String base) {
        int i = ++scopeCounter;
        
        String name = base.replaceAll("-", "_") + "-" + i;
        Scope scope = Scope.of(owner.getId(), name);
        scopes.add(scope);
        return scope;
    }
    
    @Override
    public Scope createScopeWithIdentity(String identity) {
        Scope scope = Scope.of(owner.getId(), identity);
        scopes.add(scope);
        return scope;
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
    
    @Override
    public ModuleScopeGraph createChild(IModule module, List<Scope> canExtend) {
        ModuleScopeGraph child = new ModuleScopeGraph(module, edgeLabels, dataLabels, noDataLabel, canExtend);
        return child;
    }
    
    @Override
    public ModuleScopeGraph addChild(IModule child) {
        currentModification++;
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
        return children.stream().map(s -> SolverContext.context().getModuleUnchecked(s).getScopeGraph())::iterator;
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
                        addDataIfScope(scopes, data, unifier);
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
                        addDataIfScope(scopes, data, unifier);
                    }
                }
            }
        }
        
        msg.children.addAll(this.children);
        
        //TODO also need to add associated scopes data

        
        return msg;
    }
    
    private void addDataIfScope(Queue<Scope> scopes, ITerm data, IUnifier.Immutable unifier) {
        //Try to match as a scope
        Optional<Scope> sdata = Scope.matcher().match(data, unifier);
        if (sdata.isPresent()) {
            Scope scope = sdata.get();
            
            //Do not add the scope if it is not ours
            if (!owner.getId().equals(scope.getResource())) return;
            scopes.add(sdata.get());
            return;
        }
        
        if (data instanceof ITermVar) {
            if (!unifier.isGround(data)) {
                //TODO This variable is unbound! How to handle this? We can assume it is not a scope at this moment
                System.out.println("While determining external scope graph of " + owner + ": There is data in the scope graph that is not ground!");
                return;
            }
            data = unifier.findRecursive(data);
        }
        
        //Try to match as a scope
        sdata = Scope.matcher().match(data, unifier);
        if (sdata.isPresent()) {
            System.err.println("Matching after instantiation from the unifier worked!");
            Scope scope = sdata.get();
            
            //Do not add the scope if it is not ours
            if (!owner.getId().equals(scope.getResource())) return;
            scopes.add(sdata.get());
        }
    }
    
    @Override
    public DelegatingModuleScopeGraph delegatingGraph(boolean clearScopes) {
        return new DelegatingModuleScopeGraph(this, clearScopes);
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
    
    private ModuleScopeGraph(ModuleScopeGraph original, IModule newOwner) {
        this.owner = newOwner;
        this.canExtend = original.canExtend;
        this.children.addAll(original.children);
        this.copyId = original.copyId;
        
        IRelation3.Immutable<Scope, ITerm, ITerm> nData = original.data.freeze();
        original.data = nData.melt();
        this.data = nData.melt();
        
        IRelation3.Immutable<Scope, ITerm, Scope> nEdges = original.edges.freeze();
        original.edges = nEdges.melt();
        this.edges = nEdges.melt();
        
        this.dataLabels = original.dataLabels;
        this.edgeLabels = original.edgeLabels;
        this.noDataLabel = original.noDataLabel;
        this.id = idCounter.getAndIncrement();
        this.parentScopes = original.parentScopes;
        this.scopeCounter = original.scopeCounter;
        this.scopes.addAll(original.scopes);
        this.useLock = useLock();
    }
    
    @Override
    public ModuleScopeGraph copy(IModule owner) {
        return new ModuleScopeGraph(this, owner);
    }
    
    // --------------------------------------------------------------------------------------------
    // Object methods
    // --------------------------------------------------------------------------------------------
    
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
        return "SG<" + owner.getId() + ", " + id + "_" + copyId + ">";
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
}
