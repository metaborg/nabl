package mb.statix.taico.scopegraph;

import static mb.statix.taico.solver.SolverContext.context;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.Lock;

import mb.nabl2.terms.ITerm;
import mb.nabl2.util.collections.IRelation3;
import mb.statix.scopegraph.terms.Scope;
import mb.statix.taico.module.IModule;
import mb.statix.taico.scopegraph.locking.LockManager;
import mb.statix.taico.scopegraph.reference.ModuleDelayException;

public class TrackingModuleScopeGraph extends ModuleScopeGraph implements ITrackingScopeGraph<Scope, ITerm, ITerm> {
    private final ModuleScopeGraph original;
    
    private final Map<Scope, ITerm> trackedEdges = new HashMap<>();
    private final Map<Scope, ITerm> trackedData = new HashMap<>();
    private final Set<TrackingModuleScopeGraph> trackedChildren = new HashSet<>();
    private final Map<String, ITrackingScopeGraph<Scope, ITerm, ITerm>> trackers;
    private final LockManager lockManager;
    
    private volatile int currentModification;
    
    public TrackingModuleScopeGraph(ModuleScopeGraph original) {
        this(original, new HashMap<>(), new LockManager(original.getOwner()));
    }
    
    public TrackingModuleScopeGraph(ModuleScopeGraph original, Map<String, ITrackingScopeGraph<Scope, ITerm, ITerm>> trackers, LockManager lockManager) {
        super(original.id, original.getOwner(), original.getEdgeLabels(), original.getDataLabels(), original.getNoDataLabel(), original.getParentScopes());
        this.original = original;
        
        this.trackers = trackers;
        this.lockManager = lockManager;
        this.currentModification = original.currentModification;
        
        trackers.put(getOwner().getId(), this);
        
        
        for (IMInternalScopeGraph<Scope, ITerm, ITerm> child : original.getChildren()) {
            TrackingModuleScopeGraph tmsg = (TrackingModuleScopeGraph) trackers.computeIfAbsent(child.getOwner().getId(), m -> child.trackingGraph(trackers, lockManager));
            trackedChildren.add(tmsg);
            trackers.put(child.getOwner().getId(), tmsg);
        }
    }
    
    @Override
    public IRelation3<Scope, ITerm, ITerm> getOwnData() {
        return original.getOwnData();
    }
    
    @Override
    public IRelation3<Scope, ITerm, Scope> getOwnEdges() {
        return original.getOwnEdges();
    }

    @Override
    public Set<Scope> getScopes() {
        return original.getScopes();
    }

    @Override
    public ModuleScopeGraph createChild(IModule module,
            List<Scope> canExtend) {
        return super.createChild(module, canExtend).trackingGraph(trackers, lockManager);
    }
    
    @Override
    public ModuleScopeGraph addChild(IModule module) {
        return super.addChild(module).trackingGraph(trackers, lockManager);
    }
    
    @Override
    public boolean removeChild(IModule child) {
        throw new UnsupportedOperationException("Scope graphs should not be purged while tracking them!");
    }

    @Override
    public Collection<? extends TrackingModuleScopeGraph> getChildren() {
        if (original.currentModification != this.currentModification) {
            //Add missing children
            for (IMInternalScopeGraph<Scope, ITerm, ITerm> child : original.getChildren()) {
                if (!trackedChildren.contains(child)) {
                    TrackingModuleScopeGraph tmsg = (TrackingModuleScopeGraph) trackers.computeIfAbsent(child.getOwner().getId(), m -> child.trackingGraph(trackers, lockManager));
                    trackedChildren.add(tmsg);
                }
            }
        }
        
        return trackedChildren;
    }

    @Override
    public void purgeChildren() {
        throw new UnsupportedOperationException("Scope graphs should not be purged while tracking them!");
    }
    
    @Override
    public Set<Scope> getEdges(Scope scope, ITerm label) throws ModuleDelayException {
        if (getOwner().getId().equals(scope.getResource())) {
            return getTransitiveEdges(scope, label);
        } else {
            ITrackingScopeGraph<Scope, ITerm, ITerm> tmsg =
                    trackers.computeIfAbsent(scope.getResource(), o -> context().getModuleUnchecked(o).getScopeGraph().trackingGraph(trackers, lockManager));
            return tmsg.getEdges(scope, label);
        }
    }
    
    @Override
    public Set<ITerm> getData(Scope scope, ITerm label) throws ModuleDelayException  {
        if (getOwner().getId().equals(scope.getResource())) {
            return getTransitiveData(scope, label);
        } else {
            ITrackingScopeGraph<Scope, ITerm, ITerm> tmsg =
                    trackers.computeIfAbsent(scope.getResource(), o -> context().getModuleUnchecked(o).getScopeGraph().trackingGraph(trackers, lockManager));
            return tmsg.getData(scope, label);
        }
    }
    
    @Override
    public Collection<String> getReachedModules() {
        Set<String> modules = new HashSet<>();
        for (String module : trackers.keySet()) {
            addModules(modules, context().getModuleUnchecked(module));
        }
        modules.remove(getOwner().getId());
        
        return modules;
    }
    
    private void addModules(Set<String> acc, IModule module) {
        acc.add(module.getId());
        for (IModule child : module.getChildren()) {
            addModules(acc, child);
        }
    }
    
    @Override
    public Set<Scope> getTransitiveEdges(Scope scope, ITerm label) {
        trackedEdges.put(scope, label);
        lockManager.acquire(getReadLock());
        return super._getTransitiveEdges(scope, label);
    }

    @Override
    public Set<ITerm> getTransitiveData(Scope scope, ITerm label) {
        trackedData.put(scope, label);
        lockManager.acquire(getReadLock());
        return super._getTransitiveData(scope, label);
    }
    
    @Override
    public IMExternalScopeGraph<Scope, ITerm, ITerm> externalGraph() {
        throw new UnsupportedOperationException("Cannot get external graph of a tracking scope graphOwnableScope (currently).");
    }
    
    @Override
    public Lock getReadLock() {
        return original.getReadLock();
    }
    
    @Override
    public Lock getWriteLock() {
        return original.getWriteLock();
    }
    
    //TODO IMPORTANT Does the delegating graph correctly call our methods?

    // --- tracking stuff ---
    
    @Override
    public Map<Scope, ITerm> getTrackedEdges() {
        return trackedEdges;
    }

    @Override
    public Map<Scope, ITerm> getTrackedData() {
        return trackedData;
    }
    
    @Override
    public Map<String, Map<Scope, ITerm>> aggregateTrackedEdges() {
        Map<String, Map<Scope, ITerm>> tbr = new HashMap<>();
        for (ITrackingScopeGraph<Scope, ITerm, ITerm> tmsg : trackers.values()) {
            tbr.put(tmsg.getOwner().getId(), tmsg.getTrackedEdges());
        }
        return tbr;
    }
    
    @Override
    public Map<String, Map<Scope, ITerm>> aggregateTrackedData() {
        Map<String, Map<Scope, ITerm>> tbr = new HashMap<>();
        for (ITrackingScopeGraph<Scope, ITerm, ITerm> tmsg : trackers.values()) {
            tbr.put(tmsg.getOwner().getId(), tmsg.getTrackedData());
        }
        return tbr;
    }
    
    @Override
    public LockManager getLockManager() {
        return lockManager;
    }
}