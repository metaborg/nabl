package mb.statix.taico.incremental.strategy;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import mb.statix.solver.IConstraint;
import mb.statix.solver.ISolverResult;
import mb.statix.solver.log.IDebugContext;
import mb.statix.taico.incremental.changeset.IChangeSet2;
import mb.statix.taico.incremental.changeset.QueryChangeSet;
import mb.statix.taico.incremental.manager.QueryIncrementalManager;
import mb.statix.taico.module.IModule;
import mb.statix.taico.module.ModuleCleanliness;
import mb.statix.taico.scopegraph.reference.ModuleDelayException;
import mb.statix.taico.solver.IMState;
import mb.statix.taico.solver.SolverContext;

public class QueryIncrementalStrategy extends IncrementalStrategy {
    /**
     * Reanalyzes the modules that are not marked as clean.
     */
    @Override
    public Map<String, ISolverResult> reanalyze(IChangeSet2 changeSet, IMState baseState, Map<String, IConstraint> constraints, IDebugContext debug) throws InterruptedException {
        return baseState.coordinator().solve(this, changeSet, baseState, constraints, debug);
    }
    
    @Override
    public QueryIncrementalManager createManager() {
        return new QueryIncrementalManager();
    }
    
    @Override
    public IChangeSet2 createChangeSet(SolverContext oldContext, Collection<String> added, Collection<String> changed,
            Collection<String> removed) {
        return new QueryChangeSet(oldContext, added, changed, removed);
    }
    
    @Override
    public IModule getModule(SolverContext context, SolverContext oldContext, IModule requester, String id) throws ModuleDelayException {
        IModule module = context.getModuleManager().getModule(id);
        if (module != null) return module;

        if (oldContext == null) return null;
        module = oldContext.getModuleManager().getModule(id);
        if (module == null) return null;
        
        //TODO Move to IncrementalManager?
        if (!context.<QueryIncrementalManager>getIncrementalManager().isAllowedAccess(id)) {
            throw new ModuleDelayException(id);
        }
        
        return module;
    }
    
    @Override
    public IModule getChildModule(SolverContext context, SolverContext oldContext, IModule requester, String childId) {
        //Child access works the same as normal access.
        return getModule(context, oldContext, requester, childId);
    }
    
    @Override
    public Map<IModule, IConstraint> createModulesForPhase(SolverContext context,
            IChangeSet2 changeSet,
            Map<String, IConstraint> moduleConstraints) {
        Map<IModule, IConstraint> newModules = new HashMap<>();
        for (Entry<String, IConstraint> entry : moduleConstraints.entrySet()) {
            System.err.println("[QI] Encountered entry for " + entry.getKey());
            IModule oldModule = context.getOldContext().map(c -> c.getModuleByName(entry.getKey(), 1)).orElse(null);
            
            if (oldModule == null || oldModule.getTopCleanliness() != ModuleCleanliness.CLEAN) {
                IModule module = createFileModule(context, entry.getKey(), entry.getValue());
                newModules.put(module, entry.getValue());
            } else {
                //Old module is clean, we can reuse it
                reuseOldModule(context, changeSet, oldModule);
            }
        }
        
        return newModules;
    }
    
    @Override
    protected void reuseOldModule(SolverContext context, IChangeSet2 changeSet, IModule oldModule) {
        IModule newModule = oldModule.copy();
        for (IModule child : changeSet.removed()) {
            newModule.getScopeGraph().removeChild(child);
        }
        context.addModule(newModule);
        super.reuseOldModule(context, changeSet, newModule);
    }
}