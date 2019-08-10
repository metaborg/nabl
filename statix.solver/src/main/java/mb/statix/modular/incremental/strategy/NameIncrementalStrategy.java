package mb.statix.modular.incremental.strategy;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import mb.statix.modular.dependencies.DependencyManager;
import mb.statix.modular.incremental.Flag;
import mb.statix.modular.incremental.changeset.IChangeSet;
import mb.statix.modular.incremental.changeset.NameChangeSet;
import mb.statix.modular.incremental.manager.NameIncrementalManager;
import mb.statix.modular.module.IModule;
import mb.statix.modular.module.ModuleCleanliness;
import mb.statix.modular.module.ModulePaths;
import mb.statix.modular.module.split.SplitModuleUtil;
import mb.statix.modular.ndependencies.NDependencies;
import mb.statix.modular.scopegraph.reference.ModuleDelayException;
import mb.statix.modular.solver.Context;
import mb.statix.modular.util.TOverrides;
import mb.statix.modular.util.TSettings;
import mb.statix.modular.util.TTimings;
import mb.statix.solver.IConstraint;

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
    public DependencyManager<?> createDependencyManager(Context oldContext) {
        DependencyManager<?> tbr;
        if (oldContext != null) {
            tbr = oldContext.getDependencyManager();
            
            //Check if the observers match our settings
            List<Class<?>> expected = TSettings.getDependencyObservers().stream().map(Object::getClass).collect(Collectors.toList());
            List<Class<?>> actual = tbr.getObservers().stream().map(Object::getClass).collect(Collectors.toList());
            if (!expected.equals(actual)) {
                TTimings.startPhase("Rebuilding indices");
                System.err.println("Dependency observers mismatch, rebuilding indices...");
                tbr.clearObservers();
                tbr.registerObservers(TSettings.getDependencyObservers());
                tbr.refreshObservers();
                TTimings.endPhase("Rebuilding indices");
            }
            
            return tbr;
        }
        
        tbr = new DependencyManager<>((Function<String, NDependencies> & Serializable) NDependencies::new);
        tbr.registerObservers(TSettings.getDependencyObservers());
        return tbr;
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
        
        //We do NOT transfer dependencies since we transfer the entire dependency manager for this strategy.
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
                    if (oldModule == null) {
                        module.addFlag(Flag.NEW);
                    } else {
                        //Reset the dependencies for any old module that is dirty
                        context.resetDependencies(module.getId());
                    }
                }
            } else {
                reuseOldModule(context, changeSet, oldModule, false);
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
            .forEachOrdered(module -> reuseOldModule(context, changeSet, module, false));
            
            //Transfer the split module at the top level (TODO or prevent this from being created).
            //This module cannot have any children, so we do not need to worry about that
            if (TOverrides.SPLIT_MODULES) {
                String topSplitId = SplitModuleUtil.getSplitModuleId(context.getRootModule().getId());
                IModule topSplit = oldContext.getModuleUnchecked(topSplitId);
                if (topSplit != null) {
                    reuseOldModule(context, changeSet, topSplit, false);
                    
                    //INVARIANT: The split module of the top level does not have any children
                    assert topSplit.getScopeGraph().getChildIds().isEmpty() : "The top module split should not have any children!";
                }
            }
        }
        
        return newModules;
    }
}

