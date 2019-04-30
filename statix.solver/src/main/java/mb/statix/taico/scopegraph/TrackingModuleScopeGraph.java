package mb.statix.taico.scopegraph;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.Lock;

import mb.nabl2.terms.ITerm;
import mb.nabl2.util.collections.IRelation3;
import mb.statix.scopegraph.terms.AScope;
import mb.statix.solver.Delay;
import mb.statix.taico.module.IModule;
import mb.statix.taico.scopegraph.locking.LockManager;

public class TrackingModuleScopeGraph extends ModuleScopeGraph implements ITrackingScopeGraph<AScope, ITerm, ITerm, ITerm> {
    private final ModuleScopeGraph original;
    
    private final Map<AScope, ITerm> trackedEdges = new HashMap<>();
    private final Map<AScope, ITerm> trackedData = new HashMap<>();
    private final Set<TrackingModuleScopeGraph> trackedChildren = new HashSet<>();
    private final Map<String, ITrackingScopeGraph<AScope, ITerm, ITerm, ITerm>> trackers;
    
    private volatile int currentModification;
    
    public TrackingModuleScopeGraph(ModuleScopeGraph original) {
        this(original, new HashMap<>());
    }
    
    public TrackingModuleScopeGraph(ModuleScopeGraph original, Map<String, ITrackingScopeGraph<AScope, ITerm, ITerm, ITerm>> trackers) {
        super(original.id, original.getOwner(), original.getLabels(), original.getEndOfPath(), original.getRelations(), original.getParentScopes());
        this.original = original;
        
        this.trackers = trackers;
        this.currentModification = original.currentModification;
        
        trackers.put(getOwner().getId(), this);
        
        
        for (ModuleScopeGraph child : original.getChildren()) {
            TrackingModuleScopeGraph tmsg = (TrackingModuleScopeGraph) trackers.computeIfAbsent(child.getOwner().getId(), m -> child.trackingGraph(trackers));
            trackedChildren.add(tmsg);
            trackers.put(child.getOwner().getId(), tmsg);
        }
    }
    
    @Override
    public IRelation3<AScope, ITerm, IEdge<AScope, ITerm, List<ITerm>>> getData() {
        return original.getData();
    }
    
    @Override
    public IRelation3<AScope, ITerm, IEdge<AScope, ITerm, AScope>> getEdges() {
        return original.getEdges();
    }

    @Override
    public Set<AScope> getScopes() {
        return original.getScopes();
    }

    @Override
    public ModuleScopeGraph createChild(IModule module,
            List<AScope> canExtend) {
        return super.createChild(module, canExtend).trackingGraph(trackers);
    }
    
    @Override
    public ModuleScopeGraph addChild(IModule module) {
        return super.addChild(module).trackingGraph(trackers);
    }
    
    @Override
    public boolean removeChild(IModule child) {
        throw new UnsupportedOperationException("Scope graphs should not be purged while tracking them!");
    }

    @Override
    public Collection<? extends TrackingModuleScopeGraph> getChildren() {
        if (original.currentModification != this.currentModification) {
            //Add missing children
            for (ModuleScopeGraph child : original.getChildren()) {
                if (!trackedChildren.contains(child)) {
                    TrackingModuleScopeGraph tmsg = (TrackingModuleScopeGraph) trackers.computeIfAbsent(child.getOwner().getId(), m -> child.trackingGraph(trackers));
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
    public ModuleScopeGraph deepCopy() {
        return original.deepCopy().trackingGraph(trackers);
    }

    @Override
    public void updateToCopy(IMInternalScopeGraph<AScope, ITerm, ITerm, ITerm> copy,
            boolean checkConcurrency) {
        original.updateToCopy(copy, checkConcurrency);
    }
    
    @Override
    public Set<IEdge<AScope, ITerm, AScope>> getEdges(AScope scope, ITerm label, LockManager lockManager) throws Delay {
        if (getOwner().getId().equals(scope.getResource())) {
            return getTransitiveEdges(scope, label, lockManager);
        } else {
            ITrackingScopeGraph<AScope, ITerm, ITerm, ITerm> tmsg =
                    trackers.computeIfAbsent(scope.getResource(), o -> getOwner().getContext().getModuleUnchecked(o).getScopeGraph().trackingGraph(trackers));
            return tmsg.getEdges(scope, label, lockManager);
        }
    }
    
    @Override
    public Set<IEdge<AScope, ITerm, List<ITerm>>> getData(AScope scope, ITerm label, LockManager lockManager) throws Delay  {
        if (getOwner().getId().equals(scope.getResource())) {
            return getTransitiveData(scope, label, lockManager);
        } else {
            ITrackingScopeGraph<AScope, ITerm, ITerm, ITerm> tmsg =
                    trackers.computeIfAbsent(scope.getResource(), o -> getOwner().getContext().getModuleUnchecked(o).getScopeGraph().trackingGraph(trackers));
            return tmsg.getData(scope, label, lockManager);
        }
    }
    
    @Override
    public Collection<String> getReachedModules() {
        Set<String> modules = new HashSet<>();
        for (String module : trackers.keySet()) {
            addModules(modules, getOwner().getContext().getModuleUnchecked(module));
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
    public Set<IEdge<AScope, ITerm, AScope>> getTransitiveEdges(AScope scope, ITerm label, LockManager lockManager) {
        trackedEdges.put(scope, label);
        return super.getTransitiveEdges(scope, label, lockManager);
    }

    @Override
    public Set<IEdge<AScope, ITerm, List<ITerm>>> getTransitiveData(AScope scope, ITerm label, LockManager lockManager) {
        trackedData.put(scope, label);
        return super.getTransitiveData(scope, label, lockManager);
    }
    
    @Override
    public IMExternalScopeGraph<AScope, ITerm, ITerm, ITerm> externalGraph() {
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
    public Map<AScope, ITerm> getTrackedEdges() {
        return trackedEdges;
    }

    @Override
    public Map<AScope, ITerm> getTrackedData() {
        return trackedData;
    }
    
    @Override
    public Map<String, Map<AScope, ITerm>> aggregateTrackedEdges() {
        Map<String, Map<AScope, ITerm>> tbr = new HashMap<>();
        for (ITrackingScopeGraph<AScope, ITerm, ITerm, ITerm> tmsg : trackers.values()) {
            tbr.put(tmsg.getOwner().getId(), tmsg.getTrackedEdges());
        }
        return tbr;
    }
    
    @Override
    public Map<String, Map<AScope, ITerm>> aggregateTrackedData() {
        Map<String, Map<AScope, ITerm>> tbr = new HashMap<>();
        for (ITrackingScopeGraph<AScope, ITerm, ITerm, ITerm> tmsg : trackers.values()) {
            tbr.put(tmsg.getOwner().getId(), tmsg.getTrackedData());
        }
        return tbr;
    }
}
