package mb.statix.taico.incremental.strategy;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import mb.statix.solver.IConstraint;
import mb.statix.solver.ISolverResult;
import mb.statix.solver.log.IDebugContext;
import mb.statix.taico.incremental.changeset.AChangeSet2;
import mb.statix.taico.incremental.changeset.IChangeSet;
import mb.statix.taico.incremental.changeset.IChangeSet2;
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
    public IChangeSet2 createChangeSet(SolverContext oldContext, Collection<String> added, Collection<String> changed,
            Collection<String> removed) {
        return new BaselineChangeSet(oldContext, added, changed, removed);
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
    
    public static class BaselineChangeSet extends AChangeSet2 {
        private static final long serialVersionUID = 1L;
        
        private static final ModuleCleanliness[] SUPPORTED = new ModuleCleanliness[] {
                ModuleCleanliness.CLEAN,
                ModuleCleanliness.CLIRTY,
                ModuleCleanliness.DELETED,
                ModuleCleanliness.DIRTY,
                ModuleCleanliness.CHILDOFDIRTY,
                ModuleCleanliness.NEW
        };
        
        public BaselineChangeSet(SolverContext oldContext,
                Collection<String> added, Collection<String> changed, Collection<String> removed) {
            super(oldContext, Arrays.asList(SUPPORTED), added, changed, removed);
            init(oldContext);
        }
        
        @Override
        protected void init(SolverContext oldContext) {
            //Mark all removed modules and descendants as deleted
            removed().stream().flatMap(m -> m.getDescendants()).forEach(m -> m.flagIfClean(ModuleCleanliness.DELETED));

            //#0 Compute child of dirty
            for (IModule module : dirty()) {
                add(ModuleCleanliness.CHILDOFDIRTY, true, module.getDescendants());
            }

            //#1 Compute clirty = all modules that depend on dirty or clirty modules
            //I need to flag all modules that depend on the dirty modules (recursively) as possibly dirty (clirty)
            //Using a DFS algorithm with the reverse dependency edges in the graph
            Set<IModule> visited = new HashSet<>(dirty());
            visited.addAll(childOfDirty());
            visited.addAll(removed());
            LinkedList<IModule> stack = new LinkedList<>(visited);
            while (!stack.isEmpty()) {
                IModule module = stack.pop();
                for (IModule depModule : module.getDependants().keySet()) {
                    if (visited.contains(depModule)) continue;
                    if (depModule.getFlag() != ModuleCleanliness.CLEAN) System.err.println("Cleanliness algorithm seems incorrect, encountered ");
                    visited.add(depModule);
                    add(ModuleCleanliness.CLIRTY, true, depModule);
                    stack.push(depModule);
                }
            }
            
            //All modules that depend upon removed modules are also considered clirty.
            add(ModuleCleanliness.CLIRTY, true, removed().stream().flatMap(m -> m.getDependants().keySet().stream()));

            //#2 Compute clean = all modules that were not marked otherwise
            add(ModuleCleanliness.CLEAN, false, oldContext.getModules().stream().filter(m -> m.getFlag() == ModuleCleanliness.CLEAN));

            System.err.println("Based on the files, we identified:");
            System.err.println("  Dirty:  " + dirty().size()  + " modules (" + removed().size() + " removed)");
            System.err.println("  Clirty: " + clirty().size() + " modules");
            System.err.println("  Clean:  " + clean().size()  + " modules");
        }
    }
}
