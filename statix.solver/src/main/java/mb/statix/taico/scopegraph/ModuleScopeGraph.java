package mb.statix.taico.scopegraph;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import mb.nabl2.terms.ITerm;
import mb.nabl2.util.collections.HashTrieRelation3;
import mb.nabl2.util.collections.IRelation3;
import mb.statix.scopegraph.terms.Scope;
import mb.statix.taico.module.IModule;
import mb.statix.taico.solver.SolverContext;
import mb.statix.taico.solver.concurrent.locking.DummyReadWriteLock;
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
    
    private final HashSet<Scope> scopes = new HashSet<>();
    private transient IRelation3.Transient<Scope, ITerm, Scope> edges = HashTrieRelation3.Transient.of();
    private transient IRelation3.Transient<Scope, ITerm, ITerm> data = HashTrieRelation3.Transient.of();

    protected int scopeCounter;
    protected int id;
    private int copyId;
    
    protected transient ReadWriteLock lock = TOverrides.CONCURRENT ? new ReentrantReadWriteLock() : DummyReadWriteLock.of();
    
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
        //TODO Should be possible without passing a scope, but rather something specifying the parent scope number that was passed.
        if (owner.getId().equals(scope.getResource())) {
            return getTransitiveEdges(scope, label);
        } else {
            //TODO IMPORTANT Should the requester be the owner of this scope graph? Or should it be the one asking this query?
            return Scopes.getOwner(scope, owner).getScopeGraph().getEdges(scope, label);
        }
    }
    
    @Override
    public Set<ITerm> getData(Scope scope, ITerm label) {
        if (owner.getId().equals(scope.getResource())) {
            return getTransitiveData(scope, label);
        } else {
            //TODO IMPORTANT Should the requester be the owner of this scope graph? Or should it be the one asking this query?
            return Scopes.getOwner(scope, owner).getScopeGraph().getData(scope, label);
        }
    }
    
    @Override
    public Set<Scope> getTransitiveEdges(Scope scope, ITerm label) {
//        lockManager.acquire(getReadLock());
        Set<Scope> set;
        getReadLock().lock();
        try {
            set = new HashSet<>(getOwnEdges().get(scope, label));
        } finally {
            getReadLock().unlock();
        }
        
        //TODO OPTIMIZATION We might be able to do a better check than just the scopes that are passed based on the spec. 
        for (IMInternalScopeGraph<Scope, ITerm, ITerm> child : getChildren()) {
            if (child.getExtensibleScopes().contains(scope)) {
                set.addAll(child.getTransitiveEdges(scope, label));
            }
        }
        return set;
    }
    
    /**
     * Gets transitive edges without locking.
     * 
     * @param scope
     *      the scope
     * @param label
     *      the label
     * 
     * @return
     *      the set of edges that this module and children have
     */
    protected Set<Scope> _getTransitiveEdges(Scope scope, ITerm label) {
        Set<Scope> set = new HashSet<>(getOwnEdges().get(scope, label));
        
        //TODO OPTIMIZATION We might be able to do a better check than just the scopes that are passed based on the spec. 
        for (IMInternalScopeGraph<Scope, ITerm, ITerm> child : getChildren()) {
            if (child.getExtensibleScopes().contains(scope)) {
                set.addAll(child.getTransitiveEdges(scope, label));
            }
        }
        return set;
    }

    @Override
    public Set<ITerm> getTransitiveData(Scope scope, ITerm label) {
//        lockManager.acquire(getReadLock());
        Set<ITerm> set;
        getReadLock().lock();
        try {
            set = new HashSet<>(getOwnData().get(scope, label));
        } finally {
            getReadLock().unlock();
        }
        //TODO OPTIMIZATION We might be able to do a better check than just the scopes that are passed based on the spec. 
        for (IMInternalScopeGraph<Scope, ITerm, ITerm> child : getChildren()) {
            if (child.getExtensibleScopes().contains(scope)) {
                set.addAll(child.getTransitiveData(scope, label));
            }
        }
        return set;
    }
    
    /**
     * Gets transitive data without locking.
     * 
     * @param scope
     *      the scope
     * @param label
     *      the label
     * 
     * @return
     *      the set of data that this module and children have
     */
    protected Set<ITerm> _getTransitiveData(Scope scope, ITerm label) {
      Set<ITerm> set = new HashSet<>(getOwnData().get(scope, label));

      //TODO OPTIMIZATION We might be able to do a better check than just the scopes that are passed based on the spec. 
      for (IMInternalScopeGraph<Scope, ITerm, ITerm> child : getChildren()) {
          if (child.getExtensibleScopes().contains(scope)) {
              set.addAll(child.getTransitiveData(scope, label));
          }
      }
      return set;
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
    public boolean addEdge(Scope sourceScope, ITerm label, Scope targetScope) {
        if (!getScopes().contains(sourceScope) && !getExtensibleScopes().contains(sourceScope)) {
            throw new IllegalArgumentException(
                    "addEdge directed to wrong scope graph: "
                    + "adding an edge to a scope that is not extendable by this scope graph. "
                    + "SG module: (" + this.owner + "), "
                    + "Scope: " + sourceScope + ", "
                    + "Edge: " + sourceScope + " -" + label + "-> " + targetScope);
        }
        
        getWriteLock().lock();
        try {
            return edges.put(sourceScope, label, targetScope);
        } finally {
            getWriteLock().unlock();
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
        
        getWriteLock().lock();
        try {
            return data.put(scope, relation, datum);
        } finally {
            getWriteLock().unlock();
        }
    }
    
    @Override
    public ModuleScopeGraph createChild(IModule module, List<Scope> canExtend) {
        currentModification++;
        ModuleScopeGraph child = new ModuleScopeGraph(module, edgeLabels, dataLabels, noDataLabel, canExtend);
        
        getWriteLock().lock();
        try {
            children.add(child.getOwner().getId());
            return child;
        } finally {
            getWriteLock().unlock();
        }
    }
    
    @Override
    public ModuleScopeGraph addChild(IModule child) {
        currentModification++;
        //TODO Unsafe cast
        ModuleScopeGraph childSg = (ModuleScopeGraph) child.getScopeGraph();
        
        getWriteLock().lock();
        try {
            children.add(child.getId());
            return childSg;
        } finally {
            getWriteLock().unlock();
        }
    }
    
    @Override
    public boolean removeChild(IModule child) {
        getWriteLock().lock();
        try {
            return children.remove(child.getId());
        } finally {
            getWriteLock().unlock();
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
        
        getWriteLock().lock();
        try {
            children.clear();
        } finally {
            getWriteLock().unlock();
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
        edges = newEdges;
        data = newData;
    }
    
    @Override
    public IMExternalScopeGraph<Scope, ITerm, ITerm> externalGraph() {
        ModuleScopeGraph msg = new ModuleScopeGraph(owner, edgeLabels, dataLabels, noDataLabel, parentScopes);
        for (Scope scope : parentScopes) {
            for (Entry<ITerm, Scope> e : getOwnEdges().get(scope)) {
                msg.addEdge(scope, e.getKey(), e.getValue());
            }
            
            for (Entry<ITerm, ITerm> e : getOwnData().get(scope)) {
                msg.addDatum(scope, e.getKey(), e.getValue());
            }
        }
        
        return msg;
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
        return "SG<@" + owner.getId() + ", " + id + "_" + copyId + ">";
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
        this.lock = new ReentrantReadWriteLock();
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
}
