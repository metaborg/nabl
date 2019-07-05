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

public class LockingModuleScopeGraph extends ModuleScopeGraph implements ILockingScopeGraph<Scope, ITerm, ITerm> {
    private final ModuleScopeGraph original;
    
    private final Set<LockingModuleScopeGraph> lockingChildren = new HashSet<>();
    private final Map<String, ILockingScopeGraph<Scope, ITerm, ITerm>> graphs;
    private final LockManager lockManager;
    
    public LockingModuleScopeGraph(ModuleScopeGraph original) {
        this(original, new HashMap<>(), new LockManager(original.getOwner()));
    }
    
    public LockingModuleScopeGraph(ModuleScopeGraph original, Map<String, ILockingScopeGraph<Scope, ITerm, ITerm>> graphs, LockManager lockManager) {
        super(original.id, original.getOwner(), original.getEdgeLabels(), original.getDataLabels(), original.getNoDataLabel(), original.getParentScopes());
        this.original = original;
        
        this.graphs = graphs;
        this.lockManager = lockManager;
        this.currentModification = original.currentModification;
        
        graphs.put(getOwner().getId(), this);
        
        
        for (IMInternalScopeGraph<Scope, ITerm, ITerm> child : original.getChildren()) {
            LockingModuleScopeGraph tmsg = (LockingModuleScopeGraph) graphs.computeIfAbsent(child.getOwner().getId(), m -> child.lockingGraph(graphs, lockManager));
            lockingChildren.add(tmsg);
            graphs.put(child.getOwner().getId(), tmsg);
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
        return super.createChild(module, canExtend).lockingGraph(graphs, lockManager);
    }
    
    @Override
    public ModuleScopeGraph addChild(IModule module) {
        return super.addChild(module).lockingGraph(graphs, lockManager);
    }
    
    @Override
    public boolean removeChild(IModule child) {
        throw new UnsupportedOperationException("Scope graphs should not be purged while tracking them!");
    }

    @Override
    public Collection<? extends LockingModuleScopeGraph> getChildren() {
        if (original.currentModification != this.currentModification) {
            //Add missing children
            for (IMInternalScopeGraph<Scope, ITerm, ITerm> child : original.getChildren()) {
                if (!lockingChildren.contains(child)) {
                    LockingModuleScopeGraph tmsg = (LockingModuleScopeGraph) graphs.computeIfAbsent(child.getOwner().getId(), m -> child.lockingGraph(graphs, lockManager));
                    lockingChildren.add(tmsg);
                }
            }
        }
        
        return lockingChildren;
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
            ILockingScopeGraph<Scope, ITerm, ITerm> tmsg =
                    graphs.computeIfAbsent(scope.getResource(), o -> context().getModuleUnchecked(o).getScopeGraph().lockingGraph(graphs, lockManager));
            return tmsg.getEdges(scope, label);
        }
    }
    
    @Override
    public Set<ITerm> getData(Scope scope, ITerm label) throws ModuleDelayException  {
        if (getOwner().getId().equals(scope.getResource())) {
            return getTransitiveData(scope, label);
        } else {
            ILockingScopeGraph<Scope, ITerm, ITerm> tmsg =
                    graphs.computeIfAbsent(scope.getResource(), o -> context().getModuleUnchecked(o).getScopeGraph().lockingGraph(graphs, lockManager));
            return tmsg.getData(scope, label);
        }
    }
    
    private void addModules(Set<String> acc, IModule module) {
        acc.add(module.getId());
        for (IModule child : module.getChildren()) {
            addModules(acc, child);
        }
    }
    
    @Override
    public Set<Scope> getTransitiveEdges(Scope scope, ITerm label) {
        lockManager.acquire(getReadLock());
        return super._getTransitiveEdges(scope, label);
    }

    @Override
    public Set<ITerm> getTransitiveData(Scope scope, ITerm label) {
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
    
    @Override
    public LockManager getLockManager() {
        return lockManager;
    }
}
