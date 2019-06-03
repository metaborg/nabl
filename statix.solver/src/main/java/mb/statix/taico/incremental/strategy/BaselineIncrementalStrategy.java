package mb.statix.taico.incremental.strategy;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import mb.statix.solver.IConstraint;
import mb.statix.solver.ISolverResult;
import mb.statix.solver.log.IDebugContext;
import mb.statix.taico.incremental.IChangeSet;
import mb.statix.taico.module.IModule;
import mb.statix.taico.module.ModuleCleanliness;
import mb.statix.taico.solver.IMState;
import mb.statix.taico.solver.SolverContext;

/**
 * Incremental strategy which is naive and simply redoes all modules that have changed and all
 * modules that depend on them (transitively) (dirty U clirty).
 * 
 * In other words, only modules that are unchanged and that do not transitively depend on modules
 * which are changed, are redone.
 * 
 * However, if any modules are added, we cannot guarantee that they don't affect existing modules,
 * so we redo all modules if modules are added.
 */
public class BaselineIncrementalStrategy extends IncrementalStrategy {
//    @Override
//    public void clearDirtyModules(IChangeSet changeSet, ModuleManager manager) {
//        if (!changeSet.added().isEmpty()) {
//            //We cannot guarantee that these additions don't influence existing results, so we will flag everything as dirty (except for the top module)
//            manager.retainModules(manager.topLevelModules());
//            return;
//        }
//        
//        //TODO: redo dirty and clirty, with clean in the context
//        Set<IModule> redo = Sets.union(changeSet.dirty(), changeSet.clirty());
//        if (redo.isEmpty()) return;
//
//        //Delete all modules that need to be redone as well as their children
//        for (IModule module : redo) {
//            //Invariant: global scope never changes
//            if (ModulePaths.pathSegments(module.getId(), 2).length == 1) {
//                throw new IllegalStateException("Top module is in the set to redo. This should not be correct!");
//            }
//            
//            manager.purgeModules(module);
//        }
//    }
    
    /**
     * Reanalyzes the modules that are not marked as clean.
     */
    @Override
    public Map<String, ISolverResult> reanalyze(IChangeSet changeSet, IMState baseState, Map<String, IConstraint> constraints, IDebugContext debug) throws InterruptedException {
        return baseState.coordinator().solve(this, changeSet, baseState, constraints, debug);
    }
    
    @Override
    public IModule getModule(SolverContext context, SolverContext oldContext, IModule requester, String id) {
        IModule module = context.getModuleManager().getModule(id);
        if (module != null) return module;
        
        //We need to redo everything if a file was added, otherwise we are missing dependencies.
        //TODO Instead, with the correct dependencies, we would not get dependencies on the individual children, but rather on their parent. This would mean that additions do work properly.
        if (oldContext == null || !context.getChangeSet().added().isEmpty()) return null;
        module = oldContext.getModuleManager().getModule(id);
        
        if (module == null) return null;
        if (module.getFlag() == ModuleCleanliness.CLEAN) return module;
        
        return null;
    }
    
    @Override
    public IModule getChildModule(SolverContext context, SolverContext oldContext, IModule requester, String childId) {
        //Child access works the same as normal access.
        return getModule(context, oldContext, requester, childId);
    }
    
    @Override
    public Map<IModule, IConstraint> createModulesForPhase(SolverContext context,
            IChangeSet changeSet,
            Map<String, IConstraint> moduleConstraints) {
        Map<IModule, IConstraint> newModules = new HashMap<>();
        for (Entry<String, IConstraint> entry : moduleConstraints.entrySet()) {
            System.err.println("[BI] Encountered entry for " + entry.getKey());
            IModule oldModule = context.getOldContext().map(c -> c.getModuleByName(entry.getKey(), 1)).orElse(null);
            
            if (oldModule == null || oldModule.getFlag() != ModuleCleanliness.CLEAN) {
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
    protected void reuseOldModule(SolverContext context, IChangeSet changeSet, IModule oldModule) {
        IModule newModule = oldModule.copy();
        for (IModule child : changeSet.removed()) {
            newModule.getScopeGraph().removeChild(child);
        }
        context.addModule(newModule);
        super.reuseOldModule(context, changeSet, newModule);
    }
    
    @Override
    public boolean endOfPhase(SolverContext context) {
        return false;
    }
}
