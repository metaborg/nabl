package mb.statix.taico.incremental.strategy;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import mb.statix.solver.IConstraint;
import mb.statix.taico.dependencies.DependencyManager;
import mb.statix.taico.dependencies.NameDependencies;
import mb.statix.taico.incremental.Flag;
import mb.statix.taico.incremental.changeset.IChangeSet;
import mb.statix.taico.incremental.changeset.NameChangeSet;
import mb.statix.taico.incremental.manager.NameIncrementalManager;
import mb.statix.taico.module.IModule;
import mb.statix.taico.module.ModuleCleanliness;
import mb.statix.taico.module.ModulePaths;
import mb.statix.taico.module.split.SplitModuleUtil;
import mb.statix.taico.scopegraph.reference.ModuleDelayException;
import mb.statix.taico.solver.Context;
import mb.statix.taico.util.TOverrides;

public class NameIncrementalStrategy extends IncrementalStrategy {
    
    @Override
    public NameIncrementalManager createManager() {
        return new NameIncrementalManager();
    }
    
    @Override
    public IChangeSet createChangeSet(Context oldContext, Collection<String> added, Collection<String> changed,
            Collection<String> removed) {
        return new NameChangeSet(oldContext, added, changed, removed);
    }
    
    @SuppressWarnings("unchecked")
    @Override
    public DependencyManager<?> createDependencyManager() {
        return new DependencyManager<>((Function<String, NameDependencies> & Serializable) NameDependencies::new);
    }
    
    @Override
    public IModule getModule(Context context, Context oldContext, String requester, String id) throws ModuleDelayException {
        //TODO Move this method to the incremental manager
        if (requester.equals(id)) return context.getModuleManager().getModule(requester);
        
        IModule module = context.getModuleManager().getModule(id);
        if (module != null) return module;

        if (oldContext == null) return null;
        module = oldContext.getModuleManager().getModule(id);
        if (module == null) return null;
        
        return module;
    }
    
    @Override
    public IModule getChildModule(Context context, Context oldContext, IModule requester, String childId) {
        //Child access works the same as normal access.
        return getModule(context, oldContext, requester.getId(), childId);
    }
    
    @Override
    public Map<IModule, IConstraint> createInitialModules(Context context,
            IChangeSet changeSet, Map<String, IConstraint> moduleConstraints) {
        
        System.err.println("[NIS] Transferring constraint-supplied modules...");
        Context oldContext = context.getOldContext();
        Map<IModule, IConstraint> newModules = new HashMap<>();
        Set<IModule> reuseChildren = new HashSet<>();
        moduleConstraints.entrySet().stream()
        .sorted((a, b) -> ModulePaths.INCREASING_PATH_LENGTH.compare(a.getKey(), b.getKey()))
        .forEachOrdered(entry -> {
            System.err.println("[NIS] Encountered entry for " + entry.getKey());
            IModule oldModule = oldContext == null ? null : oldContext.getModuleByNameOrId(entry.getKey());
            
            if (oldModule == null || oldModule.getTopCleanliness() == ModuleCleanliness.DIRTY) {
                IModule module = createModule(context, changeSet, entry.getKey(), entry.getValue(), oldModule);
                if (module != null) {
                    newModules.put(module, entry.getValue());
                    if (oldModule == null) module.addFlag(Flag.NEW);
                }
            } else {
                reuseOldModule(context, changeSet, oldModule);
                reuseChildren.add(oldModule);
            }
        });
        
        if (oldContext != null) {
            System.err.println("[NIS] Transferring child modules...");
            
            //Make sure we don't reuse the same module multiple times
            Set<IModule> allToReuse = new HashSet<>();
            for (IModule module : reuseChildren) {
                module.getDescendants(oldContext, m -> allToReuse.add(m));
            }
            
            //Reuse all modules, in the correct order
            allToReuse.stream()
            .sorted((a, b) -> ModulePaths.INCREASING_PATH_LENGTH.compare(a.getId(), b.getId()))
            .forEachOrdered(module -> reuseOldModule(context, changeSet, module));
            
            //Transfer the split module at the top level (TODO or prevent this from being created).
            //This module cannot have any children, so we do not need to worry about that
            if (TOverrides.SPLIT_MODULES) {
                String topSplitId = SplitModuleUtil.getSplitModuleId(context.getRootModule().getId());
                IModule topSplit = oldContext.getModuleUnchecked(topSplitId);
                if (topSplit != null) {
                    reuseOldModule(context, changeSet, topSplit);
                    
                    //INVARIANT: The split module of the top level does not have any children
                    assert topSplit.getScopeGraph().getChildIds().isEmpty() : "The top module split should not have any children!";
                }
            }
        }
        
        return newModules;
    }
}

