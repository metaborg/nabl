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
import mb.statix.taico.module.IModule;
import mb.statix.taico.scopegraph.locking.LockManager;

public class TrackingModuleScopeGraph extends ModuleScopeGraph implements ITrackingScopeGraph<IOwnableTerm, ITerm, ITerm, ITerm> {
    private final ModuleScopeGraph original;
    
    private final Map<IOwnableTerm, ITerm> trackedEdges = new HashMap<>();
    private final Map<IOwnableTerm, ITerm> trackedData = new HashMap<>();
    private final Set<TrackingModuleScopeGraph> trackedChildren = new HashSet<>();
    private final Map<IModule, ITrackingScopeGraph<IOwnableTerm, ITerm, ITerm, ITerm>> trackers;
    
    private volatile int currentModification;
    
    public TrackingModuleScopeGraph(ModuleScopeGraph original) {
        this(original, new HashMap<>());
    }
    
    public TrackingModuleScopeGraph(ModuleScopeGraph original, Map<IModule, ITrackingScopeGraph<IOwnableTerm, ITerm, ITerm, ITerm>> trackers) {
        super(original.id, original.getOwner(), original.getLabels(), original.getEndOfPath(), original.getRelations(), original.getParentScopes());
        this.original = original;
        
        this.trackers = trackers;
        this.currentModification = original.currentModification;
        
        trackers.put(getOwner(), this);
        
        
        for (ModuleScopeGraph child : original.getChildren()) {
            TrackingModuleScopeGraph tmsg = (TrackingModuleScopeGraph) trackers.computeIfAbsent(child.getOwner(), m -> child.trackingGraph(trackers));
            trackedChildren.add(tmsg);
            trackers.put(child.getOwner(), tmsg);
        }
    }
    
    @Override
    public IRelation3<IOwnableTerm, ITerm, IEdge<IOwnableTerm, ITerm, List<ITerm>>> getData() {
        return original.getData();
    }
    
    @Override
    public IRelation3<IOwnableTerm, ITerm, IEdge<IOwnableTerm, ITerm, IOwnableTerm>> getEdges() {
        return original.getEdges();
    }

    @Override
    public Set<IOwnableScope> getScopes() {
        return original.getScopes();
    }

    @Override
    public ModuleScopeGraph createChild(IModule module,
            List<IOwnableScope> canExtend) {
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
                    TrackingModuleScopeGraph tmsg = (TrackingModuleScopeGraph) trackers.computeIfAbsent(child.getOwner(), m -> child.trackingGraph(trackers));
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
    public void updateToCopy(IMInternalScopeGraph<IOwnableTerm, ITerm, ITerm, ITerm> copy,
            boolean checkConcurrency) {
        original.updateToCopy(copy, checkConcurrency);
    }
    
    @Override
    public Set<IEdge<IOwnableTerm, ITerm, IOwnableTerm>> getEdges(IOwnableTerm scope, ITerm label, LockManager lockManager) {
        if (scope.getOwner() == getOwner()) {
            return getTransitiveEdges(scope, label, lockManager);
        } else {
            ITrackingScopeGraph<IOwnableTerm, ITerm, ITerm, ITerm> tmsg =
                    trackers.computeIfAbsent(scope.getOwner(), o -> o.getScopeGraph().trackingGraph(trackers));
            return tmsg.getEdges(scope, label, lockManager);
        }
    }
    
    @Override
    public Set<IEdge<IOwnableTerm, ITerm, List<ITerm>>> getData(IOwnableTerm scope, ITerm label, LockManager lockManager) {
        if (scope.getOwner() == getOwner()) {
            return getTransitiveData(scope, label, lockManager);
        } else {
            ITrackingScopeGraph<IOwnableTerm, ITerm, ITerm, ITerm> tmsg =
                    trackers.computeIfAbsent(scope.getOwner(), o -> o.getScopeGraph().trackingGraph(trackers));
            return tmsg.getData(scope, label, lockManager);
        }
    }
    
    @Override
    public Collection<? extends IModule> getReachedModules() {
        Set<IModule> modules = new HashSet<>();
        for (IModule module : trackers.keySet()) {
            addModules(modules, module);
        }
        modules.remove(getOwner());
        
        return modules;
    }
    
    private void addModules(Set<IModule> acc, IModule module) {
        acc.add(module);
        for (IModule child : module.getChildren()) {
            addModules(acc, child);
        }
    }
    
    @Override
    public Set<IEdge<IOwnableTerm, ITerm, IOwnableTerm>> getTransitiveEdges(IOwnableTerm scope, ITerm label, LockManager lockManager) {
        trackedEdges.put(scope, label);
        return super.getTransitiveEdges(scope, label, lockManager);
    }

    @Override
    public Set<IEdge<IOwnableTerm, ITerm, List<ITerm>>> getTransitiveData(IOwnableTerm scope, ITerm label, LockManager lockManager) {
        trackedData.put(scope, label);
        return super.getTransitiveData(scope, label, lockManager);
    }
    
    @Override
    public IMExternalScopeGraph<IOwnableTerm, ITerm, ITerm, ITerm> externalGraph() {
        throw new UnsupportedOperationException("Cannot get external graph of a tracking scope graph (currently).");
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
    public Map<IOwnableTerm, ITerm> getTrackedEdges() {
        return trackedEdges;
    }

    @Override
    public Map<IOwnableTerm, ITerm> getTrackedData() {
        return trackedData;
    }
    
    @Override
    public Map<IModule, Map<IOwnableTerm, ITerm>> aggregateTrackedEdges() {
        Map<IModule, Map<IOwnableTerm, ITerm>> tbr = new HashMap<>();
        for (ITrackingScopeGraph<IOwnableTerm, ITerm, ITerm, ITerm> tmsg : trackers.values()) {
            tbr.put(tmsg.getOwner(), tmsg.getTrackedEdges());
        }
        return tbr;
    }
    
    @Override
    public Map<IModule, Map<IOwnableTerm, ITerm>> aggregateTrackedData() {
        Map<IModule, Map<IOwnableTerm, ITerm>> tbr = new HashMap<>();
        for (ITrackingScopeGraph<IOwnableTerm, ITerm, ITerm, ITerm> tmsg : trackers.values()) {
            tbr.put(tmsg.getOwner(), tmsg.getTrackedData());
        }
        return tbr;
    }
}
