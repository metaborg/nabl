package mb.statix.taico.scopegraph;

import java.util.ArrayList;
import java.util.Collection;
import java.util.ConcurrentModificationException;
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
import mb.statix.taico.util.IOwnable;
import mb.statix.taico.util.Scopes;
import mb.statix.util.Capsules;

public class ModuleScopeGraph implements IMInternalScopeGraph<AScope, ITerm, ITerm, ITerm>, IOwnable {
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
    private final HashSet<ModuleScopeGraph> children = new HashSet<>();
    private final HashSet<String> children2 = new HashSet<>();
    
    private final HashSet<AScope> scopes = new HashSet<>();
    private IRelation3.Transient<AScope, ITerm, IEdge<AScope, ITerm, AScope>> edges = HashTrieRelation3.Transient.of();
    private IRelation3.Transient<AScope, ITerm, IEdge<AScope, ITerm, List<ITerm>>> data = HashTrieRelation3.Transient.of();

    protected int scopeCounter;
    protected int id;
    private int copyId;
    private ModuleScopeGraph original;
    
    protected final transient ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    
    protected volatile int currentModification;
    
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
    public IRelation3<AScope, ITerm, IEdge<AScope, ITerm, List<ITerm>>> getData() {
        return data;
    }
    
    @Override
    public IRelation3<AScope, ITerm, IEdge<AScope, ITerm, AScope>> getEdges() {
        return edges;
    }
    
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
    public Set<IEdge<AScope, ITerm, AScope>> getEdges(AScope scope, ITerm label, LockManager lockManager) throws Delay {
        //TODO Should be possible without passing a scope, but rather something specifying the parent scope number that was passed.
        if (owner.getId().equals(scope.getResource())) {
            return getTransitiveEdges(scope, label, lockManager);
        } else {
            //TODO IMPORTANT Should the requester be the owner of this scope graph? Or should it be the one asking this query?
            return Scopes.getOwner(scope, owner).getScopeGraph().getEdges(scope, label, lockManager);
        }
    }
    
    @Override
    public Set<IEdge<AScope, ITerm, List<ITerm>>> getData(AScope scope, ITerm label, LockManager lockManager) throws Delay {
        if (owner.getId().equals(scope.getResource())) {
            return getTransitiveData(scope, label, lockManager);
        } else {
            //TODO IMPORTANT Should the requester be the owner of this scope graph? Or should it be the one asking this query?
            return Scopes.getOwner(scope, owner).getScopeGraph().getData(scope, label, lockManager);
        }
    }
    
    @Override
    public Set<IEdge<AScope, ITerm, AScope>> getTransitiveEdges(AScope scope, ITerm label, LockManager lockManager) {
        lockManager.acquire(getReadLock());
        Set<IEdge<AScope, ITerm, AScope>> set = new HashSet<>(getEdges().get(scope, label));
        //TODO OPTIMIZATION We might be able to do a better check than just the scopes that are passed based on the spec. 
        for (ModuleScopeGraph child : getChildren()) {
            if (child.getExtensibleScopes().contains(scope)) {
                set.addAll(child.getTransitiveEdges(scope, label, lockManager));
            }
        }
        return set;
    }

    @Override
    public Set<IEdge<AScope, ITerm, List<ITerm>>> getTransitiveData(AScope scope, ITerm label, LockManager lockManager) {
        lockManager.acquire(getReadLock());
        Set<IEdge<AScope, ITerm, List<ITerm>>> set = new HashSet<>(getData().get(scope, label));
        //TODO OPTIMIZATION We might be able to do a better check than just the scopes that are passed based on the spec. 
        for (ModuleScopeGraph child : getChildren()) {
            if (child.getExtensibleScopes().contains(scope)) {
                set.addAll(child.getTransitiveData(scope, label, lockManager));
            }
        }
        return set;
    }
    
    @Override
    public AScope createScope(String base) {
        System.err.println("[" + owner.getId() + "] Creating scope " + base);
        int i = ++scopeCounter;
        
        String name = base.replaceAll("-", "_") + "-" + i;
        AScope scope = Scope.of(owner.getId(), name);
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
    public boolean addDatum(AScope scope, ITerm relation, Iterable<ITerm> datum) {
        if (!scope.getResource().equals(this.owner.getId())) {
            System.out.println("Adding datum edge from unowned scope (@" + scope.getResource() + ") in " + this.owner);
        }
        if (!getScopes().contains(scope) && !getExtensibleScopes().contains(scope)) {
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
        
        getWriteLock().lock();
        try {
            IEdge<AScope, ITerm, List<ITerm>> edge = new Edge<>(scope, relation, datumlist);
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
            children.add(child);
            children2.add(child.getOwner().getId());
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
            children.add(childSg);
            children2.add(child.getId());
            return childSg;
        } finally {
            getWriteLock().unlock();
        }
    }
    
    @Override
    public boolean removeChild(IModule child) {
        getWriteLock().lock();
        try {
            return children.remove(child.getScopeGraph()) && children2.remove(child.getId());
        } finally {
            getWriteLock().unlock();
        }
    }
    
    @Override
    public Collection<? extends ModuleScopeGraph> getChildren() {
        return children;
    }
    
    @Override
    public void purgeChildren() {
        for (ModuleScopeGraph childSg : children) {
            childSg.purgeChildren();
        }
        
        getWriteLock().lock();
        try {
            children.clear();
        } finally {
            getWriteLock().unlock();
        }
    }
    
    @Override
    public synchronized ModuleScopeGraph deepCopy() {
        ModuleScopeGraph msg = new ModuleScopeGraph(id, owner, labels, endOfPath, relations, parentScopes);
        msg.original = this;
        msg.copyId = this.copyId + 1;
        msg.scopeCounter = this.scopeCounter;
        msg.scopes.addAll(getScopes());
        msg.edges.putAll(getEdges());
        msg.data.putAll(getData());
        for (ModuleScopeGraph child : getChildren()) {
            msg.children.add(child.deepCopy());
        }
        
        return msg;
    }
    
    @Override
    public synchronized void updateToCopy(IMInternalScopeGraph<AScope, ITerm, ITerm, ITerm> copyI, boolean checkConcurrency) {
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
                ModuleScopeGraph copyChildCopy = copyChild.deepCopy();
                copyChildCopy.original = copyChild.original; //Mark the new child as the original
                copyChild.original = copyChildCopy; //Create a link from the copy to the "original"
                this.children.add(copyChildCopy);
            }
        }
    }
    
//    @Override
//    public IMInternalScopeGraph<IOwnableTerm, ITerm, ITerm, ITerm> copy(IModule newOwner) {
//        ModuleScopeGraph msg = new ModuleScopeGraph(id, owner, labels, endOfPath, relations, parentScopes);
//        msg.original = this;
//        msg.copyId = this.copyId + 1;
//        msg.scopeCounter = this.scopeCounter;
//        msg.scopes.addAll(this.scopes);
//        msg.edges.putAll(this.edges);
//        msg.data.putAll(this.data);
//        
//        //TODO Children also new identities, and cannot be copied at this point.
//        for (ModuleScopeGraph child : this.children) {
//            msg.children.add(child.deepCopy());
//        }
//        
//        //return msg;
//    }
    
    @Deprecated
    @Override
    public synchronized void substitute(List<? extends AScope> newScopes) {
        List<? extends AScope> oldScopes = parentScopes;
        parentScopes = newScopes;
        //scopes should be stored as strings in the sets to avoid substitution
        IRelation3.Transient<AScope, ITerm, IEdge<AScope, ITerm, AScope>> newEdges = HashTrieRelation3.Transient.of();
        IRelation3.Transient<AScope, ITerm, IEdge<AScope, ITerm, List<ITerm>>> newData = HashTrieRelation3.Transient.of();
        for (int i = 0; i < oldScopes.size(); i++) {
            AScope oldScope = oldScopes.get(i);
            AScope newScope = parentScopes.get(i);
            for (Entry<ITerm, IEdge<AScope, ITerm, AScope>> e : getEdges().get(oldScope)) {
                newEdges.put(newScope, e.getKey(), new Edge<AScope, ITerm, AScope>(
                        newScope,
                        e.getValue().getLabel(),
                        e.getValue().getTarget()));
            }
            for (Entry<ITerm, IEdge<AScope, ITerm, List<ITerm>>> e : getData().get(oldScope)) {
                newData.put(newScope, e.getKey(), new Edge<AScope, ITerm, List<ITerm>>(
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
            
            for (Entry<ITerm, IEdge<AScope, ITerm, List<ITerm>>> e : getData().get(scope)) {
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
    public TrackingModuleScopeGraph trackingGraph(Map<String, ITrackingScopeGraph<AScope, ITerm, ITerm, ITerm>> trackers) {
        return new TrackingModuleScopeGraph(this, trackers);
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
    
    private static <A, B, C> boolean containsAll(IRelation3<A, B, C> haystack, IRelation3<A, B, C> needle) {
        //If there is ANY element that is not contained (anyMatch(c -> !c) == true), then the result should be false.
        return !needle.stream(haystack::contains).anyMatch(c -> !c);
    }
    
//    private class TrackingModuleScopeGraph implements ITrackingScopeGraph<IOwnableTerm, ITerm, ITerm, ITerm> {
//
//        private final Map<IOwnableTerm, ITerm> trackedEdges = new HashMap<>();
//        private final Map<IOwnableTerm, ITerm> trackedData = new HashMap<>();
//        private final Set<ITrackingScopeGraph<IOwnableTerm, ITerm, ITerm, ITerm>> trackedChildren = new HashSet<>(children.size());
//        private final Map<IModule, ITrackingScopeGraph<IOwnableTerm, ITerm, ITerm, ITerm>> trackers;
//        
//        private volatile int currentModification;
//        
//        public TrackingModuleScopeGraph() {
//            this(new HashMap<>());
//        }
//        
//        public TrackingModuleScopeGraph(Map<IModule, ITrackingScopeGraph<IOwnableTerm, ITerm, ITerm, ITerm>> trackers) {
//            this.trackers = trackers;
//            this.currentModification = ModuleScopeGraph.this.currentModification;
//            
//            trackers.put(owner, this);
//            for (ModuleScopeGraph child : children) {
//                ITrackingScopeGraph<IOwnableTerm, ITerm, ITerm, ITerm> tmsg =
//                        trackers.computeIfAbsent(child.owner, m -> child.trackingGraph(trackers));
//                trackedChildren.add(tmsg);
//                trackers.put(child.owner, tmsg);
//            }
//        }
//        
//        @Override
//        public IModule getOwner() {
//            return owner;
//        }
//        
//        @Override
//        public ITerm getEndOfPath() {
//            return endOfPath;
//        }
//
//        @Override
//        public Immutable<? extends ITerm> getLabels() {
//            return labels;
//        }
//
//        @Override
//        public Immutable<? extends ITerm> getRelations() {
//            return relations;
//        }
//        
//        @Override
//        public IRelation3<IOwnableTerm, ITerm, IEdge<IOwnableTerm, ITerm, List<ITerm>>> getData() {
//            return ModuleScopeGraph.this.getData();
//        }
//        
//        @Override
//        public IRelation3<IOwnableTerm, ITerm, IEdge<IOwnableTerm, ITerm, IOwnableTerm>> getEdges() {
//            return ModuleScopeGraph.this.getEdges();
//        }
//
//        @Override
//        public Set<? extends IOwnableTerm> getScopes() {
//            return ModuleScopeGraph.this.getScopes();
//        }
//
//        @Override
//        public Immutable<? extends IOwnableTerm> getExtensibleScopes() {
//            return canExtend;
//        }
//        
//        @Override
//        public List<? extends IOwnableScope> getParentScopes() {
//            return parentScopes;
//        }
//
//        @Override
//        public IMInternalScopeGraph<IOwnableTerm, ITerm, ITerm, ITerm> createChild(IModule module,
//                List<IOwnableScope> canExtend) {
//            return ModuleScopeGraph.this.createChild(module, canExtend).trackingGraph(trackers);
//        }
//        
//        @Override
//        public IMInternalScopeGraph<IOwnableTerm, ITerm, ITerm, ITerm> addChild(IModule module) {
//            return ModuleScopeGraph.this.addChild(module).trackingGraph(trackers);
//        }
//        
//        @Override
//        public boolean removeChild(IModule child) {
//            throw new UnsupportedOperationException("Scope graphs should not be purged while tracking them!");
//        }
//
//        @SuppressWarnings("unlikely-arg-type") //Intentional equality between tracking and non tracking graphs
//        @Override
//        public Collection<ITrackingScopeGraph<IOwnableTerm, ITerm, ITerm, ITerm>> getChildren() {
//            if (ModuleScopeGraph.this.currentModification != this.currentModification) {
//                //Add missing children
//                for (ModuleScopeGraph child : children) {
//                    if (!trackedChildren.contains(child)) {
//                        ITrackingScopeGraph<IOwnableTerm, ITerm, ITerm, ITerm> tmsg = child.trackingGraph(trackers);
//                        trackedChildren.add(tmsg);
//                        trackers.put(child.owner, tmsg);
//                    }
//                }
//            }
//            
//            return trackedChildren;
//        }
//
//        @Override
//        public void purgeChildren() {
//            throw new UnsupportedOperationException("Scope graphs should not be purged while tracking them!");
//        }
//
//        @Override
//        public IMInternalScopeGraph<IOwnableTerm, ITerm, ITerm, ITerm> deepCopy() {
//            return ModuleScopeGraph.this.deepCopy().trackingGraph(trackers);
//        }
//
//        @Override
//        public void updateToCopy(IMInternalScopeGraph<IOwnableTerm, ITerm, ITerm, ITerm> copy,
//                boolean checkConcurrency) {
//            ModuleScopeGraph.this.updateToCopy(copy, checkConcurrency);
//        }
//        
//        @Override
//        public Set<IEdge<IOwnableTerm, ITerm, IOwnableTerm>> getEdges(IOwnableTerm scope, ITerm label, LockManager lockManager) {
//            if (scope.getOwner() == owner) {
//                return getTransitiveEdges(scope, label, lockManager);
//            } else {
//                ITrackingScopeGraph<IOwnableTerm, ITerm, ITerm, ITerm> tmsg =
//                        trackers.computeIfAbsent(scope.getOwner(), o -> o.getScopeGraph().trackingGraph(trackers));
//                return tmsg.getEdges(scope, label, lockManager);
//            }
//        }
//        
//        @Override
//        public Set<IEdge<IOwnableTerm, ITerm, List<ITerm>>> getData(IOwnableTerm scope, ITerm label, LockManager lockManager) {
//            if (scope.getOwner() == owner) {
//                return getTransitiveData(scope, label, lockManager);
//            } else {
//                ITrackingScopeGraph<IOwnableTerm, ITerm, ITerm, ITerm> tmsg =
//                        trackers.computeIfAbsent(scope.getOwner(), o -> o.getScopeGraph().trackingGraph(trackers));
//                return tmsg.getData(scope, label, lockManager);
//            }
//        }
//        
//        @Override
//        public Collection<? extends IModule> getReachedModules() {
//            Set<IModule> modules = new HashSet<>();
//            for (IModule module : trackers.keySet()) {
//                addModules(modules, module);
//            }
//            modules.remove(owner);
//            
//            return modules;
//        }
//        
//        private void addModules(Set<IModule> acc, IModule module) {
//            acc.add(module);
//            for (IModule child : module.getChildren()) {
//                addModules(acc, child);
//            }
//        }
//        
//        @Override
//        public Set<IEdge<IOwnableTerm, ITerm, IOwnableTerm>> getTransitiveEdges(IOwnableTerm scope, ITerm label, LockManager lockManager) {
//            trackedEdges.put(scope, label);
//            
//            lockManager.acquire(getReadLock());
//            Set<IEdge<IOwnableTerm, ITerm, IOwnableTerm>> set = new HashSet<>(getEdges().get(scope, label));
//            //TODO OPTIMIZATION We might be able to do a better check than just the scopes that are passed based on the spec. 
//            for (ITrackingScopeGraph<IOwnableTerm, ITerm, ITerm, ITerm> child : getChildren()) {
//                if (child.getExtensibleScopes().contains(scope)) {
//                    set.addAll(child.getTransitiveEdges(scope, label, lockManager));
//                }
//            }
//            return set;
//        }
//
//        @Override
//        public Set<IEdge<IOwnableTerm, ITerm, List<ITerm>>> getTransitiveData(IOwnableTerm scope, ITerm label, LockManager lockManager) {
//            trackedData.put(scope, label);
//            
//            lockManager.acquire(getReadLock());
//            Set<IEdge<IOwnableTerm, ITerm, List<ITerm>>> set = new HashSet<>(getData().get(scope, label));
//            //TODO OPTIMIZATION We might be able to do a better check than just the scopes that are passed based on the spec. 
//            for (ITrackingScopeGraph<IOwnableTerm, ITerm, ITerm, ITerm> child : getChildren()) {
//                if (child.getExtensibleScopes().contains(scope)) {
//                    set.addAll(child.getTransitiveData(scope, label, lockManager));
//                }
//            }
//            return set;
//        }
//
//        @Override
//        public boolean addEdge(IOwnableTerm sourceScope, ITerm label, IOwnableTerm targetScope) {
//            return ModuleScopeGraph.this.addEdge(sourceScope, label, targetScope);
//        }
//
//        @Override
//        public boolean addDatum(IOwnableTerm scope, ITerm relation, Iterable<ITerm> datum) {
//            return ModuleScopeGraph.this.addDatum(scope, relation, datum);
//        }
//
//        @Override
//        public IOwnableTerm createScope(String base) {
//            return ModuleScopeGraph.this.createScope(base);
//        }
//        
////        @Override
////        public IMInternalScopeGraph<IOwnableTerm, ITerm, ITerm, ITerm> recreate(List<IOwnableTerm> newScopes) {
////            throw new UnsupportedOperationException("Scope graphs should not be cloned while tracking them!");
////        }
//        
//        @Override
//        public void substitute(List<? extends IOwnableTerm> newScopes) {
//            ModuleScopeGraph.this.substitute(newScopes);
//        }
//        
//        @Override
//        public IMExternalScopeGraph<IOwnableTerm, ITerm, ITerm, ITerm> externalGraph() {
//            return ModuleScopeGraph.this.externalGraph();
//        }
//        
//        @Override
//        public Lock getReadLock() {
//            return ModuleScopeGraph.this.getReadLock();
//        }
//        
//        @Override
//        public Lock getWriteLock() {
//            return ModuleScopeGraph.this.getWriteLock();
//        }
//        
//        @Override
//        public ITrackingScopeGraph<IOwnableTerm, ITerm, ITerm, ITerm> trackingGraph() {
//            return ModuleScopeGraph.this.trackingGraph();
//        }
//        
//        @Override
//        public ITrackingScopeGraph<IOwnableTerm, ITerm, ITerm, ITerm> trackingGraph(Map<IModule, ITrackingScopeGraph<IOwnableTerm, ITerm, ITerm, ITerm>> trackers) {
//            return ModuleScopeGraph.this.trackingGraph(trackers);
//        }
//        
//        @Override
//        public IMInternalScopeGraph<IOwnableTerm, ITerm, ITerm, ITerm> delegatingGraph() {
//            return new DelegatingModuleScopeGraph(this);
//        }
//
//        // --- tracking stuff ---
//        
//        @Override
//        public Map<IOwnableTerm, ITerm> getTrackedEdges() {
//            return trackedEdges;
//        }
//
//        @Override
//        public Map<IOwnableTerm, ITerm> getTrackedData() {
//            return trackedData;
//        }
//        
//        @Override
//        public Map<IModule, Map<IOwnableTerm, ITerm>> aggregateTrackedEdges() {
//            Map<IModule, Map<IOwnableTerm, ITerm>> tbr = new HashMap<>();
//            for (ITrackingScopeGraph<IOwnableTerm, ITerm, ITerm, ITerm> tmsg : trackers.values()) {
//                tbr.put(tmsg.getOwner(), tmsg.getTrackedEdges());
//            }
//            return tbr;
//        }
//        
//        @Override
//        public Map<IModule, Map<IOwnableTerm, ITerm>> aggregateTrackedData() {
//            Map<IModule, Map<IOwnableTerm, ITerm>> tbr = new HashMap<>();
//            for (ITrackingScopeGraph<IOwnableTerm, ITerm, ITerm, ITerm> tmsg : trackers.values()) {
//                tbr.put(tmsg.getOwner(), tmsg.getTrackedData());
//            }
//            return tbr;
//        }
//        
//        // --- ---
//        
//        @Override
//        public int hashCode() {
//            return ModuleScopeGraph.this.hashCode();
//        }
//        
//        @Override
//        public boolean equals(Object obj) {
//            if (obj == this) return true;
//            //Intentionally make TrackingModuleScopeGraphs equal to their non tracking variants.
//            return ModuleScopeGraph.this.equals(obj);
//        }
//    }
}
