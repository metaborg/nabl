package mb.statix.taico.scopegraph;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import io.usethesource.capsule.Set.Immutable;
import mb.nabl2.terms.ITerm;
import mb.nabl2.util.collections.HashTrieRelation3;
import mb.nabl2.util.collections.IRelation3;
import mb.statix.scopegraph.terms.AScope;
import mb.statix.scopegraph.terms.Scope;
import mb.statix.solver.Delay;
import mb.statix.taico.module.IModule;
import mb.statix.taico.scopegraph.locking.LockManager;
import mb.statix.taico.solver.SolverContext;
import mb.statix.taico.util.IOwnable;
import mb.statix.taico.util.Scopes;
import mb.statix.util.Capsules;

public class ModuleScopeGraph implements IMInternalScopeGraph<AScope, ITerm, ITerm, ITerm>, IOwnable {
    private static final long serialVersionUID = 1L;
    
    private static AtomicInteger idCounter = new AtomicInteger();
    //Constants for this module
    private final IModule owner;
    private final Immutable<? extends ITerm> labels;
    private final ITerm endOfPath;
    private final Immutable<? extends ITerm> relations;
    /** Scopes from parent that you can extend. Used for checking if an edge addition is valid. */
    private final Immutable<? extends AScope> canExtend;
    private List<? extends AScope> parentScopes;
    
    //Scope graph graph
    private final HashSet<String> children = new HashSet<>();
    
    private final HashSet<AScope> scopes = new HashSet<>();
    private IRelation3.Transient<AScope, ITerm, IEdge<AScope, ITerm, AScope>> edges = HashTrieRelation3.Transient.of();
    private IRelation3.Transient<AScope, ITerm, IEdge<AScope, ITerm, ITerm>> data = HashTrieRelation3.Transient.of();

    protected int scopeCounter;
    protected int id;
    private int copyId;
    
    protected final transient ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    
    protected volatile transient int currentModification;
    
    public ModuleScopeGraph(
            IModule owner,
            Iterable<? extends ITerm> labels,
            ITerm endOfPath,
            Iterable<? extends ITerm> relations,
            List<? extends AScope> canExtend) {
        this(idCounter.getAndIncrement(), owner, labels, endOfPath, relations, canExtend);
    }
    
    protected ModuleScopeGraph(
            int id,
            IModule owner,
            Iterable<? extends ITerm> labels,
            ITerm endOfPath,
            Iterable<? extends ITerm> relations,
            List<? extends AScope> canExtend) {
        this.id = id;
        this.owner = owner;
        this.labels = labels instanceof Immutable ? (Immutable<? extends ITerm>) labels : Capsules.newSet(labels);
        this.endOfPath = endOfPath;
        assert this.labels.contains(endOfPath);
        this.relations = relations instanceof Immutable ? (Immutable<? extends ITerm>) relations : Capsules.newSet(relations);
        this.parentScopes = canExtend;
        this.canExtend = Capsules.newSet(canExtend);
    }
    
    @Override
    public IModule getOwner() {
        return owner;
    }
    
    @Override
    public ITerm getEndOfPath() {
        return endOfPath;
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
    public IRelation3<AScope, ITerm, IEdge<AScope, ITerm, ITerm>> getData() {
        return data;
    }
    
    @Override
    public IRelation3<AScope, ITerm, IEdge<AScope, ITerm, AScope>> getEdges() {
        return edges;
    }
    
    @Override
    public Set<AScope> getScopes() {
        return scopes;
    }
    
    @Override
    public Immutable<? extends AScope> getExtensibleScopes() {
        return canExtend;
    }
    
    @Override
    public List<? extends AScope> getParentScopes() {
        return parentScopes;
    }

    @Override
    public Set<IEdge<AScope, ITerm, AScope>> getEdges(AScope scope, ITerm label) throws Delay {
        //TODO Should be possible without passing a scope, but rather something specifying the parent scope number that was passed.
        if (owner.getId().equals(scope.getResource())) {
            return getTransitiveEdges(scope, label);
        } else {
            //TODO IMPORTANT Should the requester be the owner of this scope graph? Or should it be the one asking this query?
            return Scopes.getOwner(scope, owner).getScopeGraph().getEdges(scope, label);
        }
    }
    
    @Override
    public Set<IEdge<AScope, ITerm, ITerm>> getData(AScope scope, ITerm label) throws Delay {
        if (owner.getId().equals(scope.getResource())) {
            return getTransitiveData(scope, label);
        } else {
            //TODO IMPORTANT Should the requester be the owner of this scope graph? Or should it be the one asking this query?
            return Scopes.getOwner(scope, owner).getScopeGraph().getData(scope, label);
        }
    }
    
    @Override
    public Set<IEdge<AScope, ITerm, AScope>> getTransitiveEdges(AScope scope, ITerm label) {
//        lockManager.acquire(getReadLock());
        Set<IEdge<AScope, ITerm, AScope>> set;
        getReadLock().lock();
        try {
            set = new HashSet<>(getEdges().get(scope, label));
        } finally {
            getReadLock().unlock();
        }
        
        //TODO OPTIMIZATION We might be able to do a better check than just the scopes that are passed based on the spec. 
        for (IMInternalScopeGraph<AScope, ITerm, ITerm, ITerm> child : getChildren()) {
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
    protected Set<IEdge<AScope, ITerm, AScope>> _getTransitiveEdges(AScope scope, ITerm label) {
        Set<IEdge<AScope, ITerm, AScope>> set = new HashSet<>(getEdges().get(scope, label));
        
        //TODO OPTIMIZATION We might be able to do a better check than just the scopes that are passed based on the spec. 
        for (IMInternalScopeGraph<AScope, ITerm, ITerm, ITerm> child : getChildren()) {
            if (child.getExtensibleScopes().contains(scope)) {
                set.addAll(child.getTransitiveEdges(scope, label));
            }
        }
        return set;
    }

    @Override
    public Set<IEdge<AScope, ITerm, ITerm>> getTransitiveData(AScope scope, ITerm label) {
//        lockManager.acquire(getReadLock());
        Set<IEdge<AScope, ITerm, ITerm>> set;
        getReadLock().lock();
        try {
            set = new HashSet<>(getData().get(scope, label));
        } finally {
            getReadLock().unlock();
        }
        //TODO OPTIMIZATION We might be able to do a better check than just the scopes that are passed based on the spec. 
        for (IMInternalScopeGraph<AScope, ITerm, ITerm, ITerm> child : getChildren()) {
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
    protected Set<IEdge<AScope, ITerm, ITerm>> _getTransitiveData(AScope scope, ITerm label) {
      Set<IEdge<AScope, ITerm, ITerm>> set = new HashSet<>(getData().get(scope, label));

      //TODO OPTIMIZATION We might be able to do a better check than just the scopes that are passed based on the spec. 
      for (IMInternalScopeGraph<AScope, ITerm, ITerm, ITerm> child : getChildren()) {
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
    public boolean addEdge(AScope sourceScope, ITerm label, AScope targetScope) {
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
            IEdge<AScope, ITerm, AScope> edge = new Edge<>(sourceScope, label, targetScope);
            return edges.put(sourceScope, label, edge);
        } finally {
            getWriteLock().unlock();
        }
    }

    @Override
    public boolean addDatum(AScope scope, ITerm relation, ITerm datum) {
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
            IEdge<AScope, ITerm, ITerm> edge = new Edge<>(scope, relation, datum);
            return data.put(scope, relation, edge);
        } finally {
            getWriteLock().unlock();
        }
    }
    
    @Override
    public ModuleScopeGraph createChild(IModule module, List<AScope> canExtend) {
        currentModification++;
        ModuleScopeGraph child = new ModuleScopeGraph(module, labels, endOfPath, relations, canExtend);
        
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
    public Iterable<? extends IMInternalScopeGraph<AScope, ITerm, ITerm, ITerm>> getChildren() {
        return children.stream().map(s -> SolverContext.context().getModuleUnchecked(s).getScopeGraph())::iterator;
    }
    
    @Override
    public void purgeChildren() {
        for (IMInternalScopeGraph<AScope, ITerm, ITerm, ITerm> childSg : getChildren()) {
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
    public synchronized void substitute(List<? extends AScope> newScopes) {
        if (parentScopes.equals(newScopes)) {
            System.err.println("Skipping substitution of scopes, no substitution necessary.");
            return;
        }
        //Sometimes the order of constraints changes the scope numbers, so substitution is necessary.
        List<? extends AScope> oldScopes = parentScopes;
        parentScopes = newScopes;
        //scopes should be stored as strings in the sets to avoid substitution
        IRelation3.Transient<AScope, ITerm, IEdge<AScope, ITerm, AScope>> newEdges = HashTrieRelation3.Transient.of();
        IRelation3.Transient<AScope, ITerm, IEdge<AScope, ITerm, ITerm>> newData = HashTrieRelation3.Transient.of();
        for (int i = 0; i < oldScopes.size(); i++) {
            AScope oldScope = oldScopes.get(i);
            AScope newScope = parentScopes.get(i);
            for (Entry<ITerm, IEdge<AScope, ITerm, AScope>> e : getEdges().get(oldScope)) {
                newEdges.put(newScope, e.getKey(), new Edge<AScope, ITerm, AScope>(
                        newScope,
                        e.getValue().getLabel(),
                        e.getValue().getTarget()));
            }
            for (Entry<ITerm, IEdge<AScope, ITerm, ITerm>> e : getData().get(oldScope)) {
                newData.put(newScope, e.getKey(), new Edge<AScope, ITerm, ITerm>(
                        newScope,
                        e.getValue().getLabel(),
                        e.getValue().getTarget()));
            }
        }
        edges = newEdges;
        data = newData;
    }
    
    @Override
    public IMExternalScopeGraph<AScope, ITerm, ITerm, ITerm> externalGraph() {
        ModuleScopeGraph msg = new ModuleScopeGraph(owner, labels, endOfPath, relations, parentScopes);
        for (AScope scope : parentScopes) {
            for (Entry<ITerm, IEdge<AScope, ITerm, AScope>> e : getEdges().get(scope)) {
                msg.addEdge(scope, e.getKey(), e.getValue().getTarget());
            }
            
            for (Entry<ITerm, IEdge<AScope, ITerm, ITerm>> e : getData().get(scope)) {
                msg.addDatum(scope, e.getKey(), e.getValue().getTarget());
            }
        }
        
        return msg;
    }
    
    @Override
    public TrackingModuleScopeGraph trackingGraph() {
        return new TrackingModuleScopeGraph(this);
    }
    
    @Override
    public TrackingModuleScopeGraph trackingGraph(Map<String, ITrackingScopeGraph<AScope, ITerm, ITerm, ITerm>> trackers, LockManager lockManager) {
        return new TrackingModuleScopeGraph(this, trackers, lockManager);
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
}
