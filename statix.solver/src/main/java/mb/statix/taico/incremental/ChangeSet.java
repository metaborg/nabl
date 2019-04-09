package mb.statix.taico.incremental;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;
import java.util.stream.Collectors;

import mb.statix.taico.incremental.strategy.IncrementalStrategy;
import mb.statix.taico.module.IModule;
import mb.statix.taico.module.ModuleCleanliness;
import mb.statix.taico.module.ModuleManager;

public interface ChangeSet {
    Set<IModule> removed();
    
    Set<IModule> changed();
    
    /**
     * Sets up the given manager for reanalysis.
     * 
     * @param manager
     *      the manager
     * @param strategy
     *      the incremental strategy
     */
    default void setupReanalysis(ModuleManager manager, IncrementalStrategy strategy) {
        //Mark all modules as clean
        manager.getModules().forEach(m -> m.flag(ModuleCleanliness.CLEAN));
        
        //Phase 1: Determine set that is definitely dirty.
        //This is all changed modules and every module depending on a removed module.
        //All these modules need to be flagged as dirty
        Set<IModule> dirty = new HashSet<>();
        for (IModule module : changed()) {
            module.flag(ModuleCleanliness.DIRTY);
            dirty.add(module);
        }
        removed().stream().flatMap(m -> m.getDependants().keySet().stream()).forEach(module -> {
            module.flag(ModuleCleanliness.DIRTY);
            dirty.add(module);
        });
        
        Set<IModule> clirty = new HashSet<>();
        
        //I need to flag all modules that depend on the dirty modules (recursively) as possibly dirty (clirty)
        //Using a DFS algorithm with the reverse dependency edges in the graph
        Set<IModule> visited = new HashSet<>(dirty);
        LinkedList<IModule> stack = new LinkedList<>(dirty);
        while (!stack.isEmpty()) {
            IModule module = stack.pop();
            for (IModule depModule : module.getDependants().keySet()) {
                if (visited.contains(depModule)) continue;
                visited.add(depModule);
                depModule.flag(ModuleCleanliness.CLIRTY);
                clirty.add(depModule);
                stack.push(depModule);
            }
        }
        
        Set<IModule> clean = manager.getModules().stream().filter(m -> m.getFlag() == ModuleCleanliness.CLEAN).collect(Collectors.toSet());
        
        System.err.println("Based on the files, we identified:");
        System.err.println("  Dirty:  " + dirty.size()  + " modules (" + removed().size() + " removed)");
        System.err.println("  Clirty: " + clirty.size() + " modules");
        System.err.println("  Clean:  " + clean.size()  + " modules");
        
        //What are the semantics of the reanalysis?
        //First we want to collect the set of modules that needs to be redone for sure.
        //  - All modules dependent on removed modules need to be redone
        //  - All changed modules need to replace their old instances

        //Determine what has changed?
        //The spec needs to be executed from the module boundary again, on the new AST.
        //The new SG needs to be swapped in.
        //Queries of dependent modules (reversed relation?) need to be rechecked.
        //So their old results also need to be stored
        
        Set<IModule> unchanged = new HashSet<>(manager.getModules());
        unchanged.removeAll(removed());
        unchanged.removeAll(changed());
        strategy.setupReanalysis(manager, unchanged, dirty, clirty, clean);
    }
}
