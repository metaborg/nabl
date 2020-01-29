package mb.statix.modular.incremental.changeset;

import static mb.statix.modular.module.ModuleCleanliness.*;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;

import mb.statix.modular.incremental.Flag;
import mb.statix.modular.module.IModule;
import mb.statix.modular.module.ModuleCleanliness;
import mb.statix.modular.solver.Context;
import mb.statix.modular.util.Scopes;
import mb.statix.modular.util.TDebug;
import mb.statix.scopegraph.terms.Scope;

public class BaselineChangeSet extends AChangeSet {
    private static final long serialVersionUID = 1L;
    
    private static final ModuleCleanliness[] SUPPORTED = new ModuleCleanliness[] {
            CLEAN,
            UNSURE,
            DELETED,
            DIRTY,
            NEW
    };
    
    public BaselineChangeSet(Context oldContext,
            Collection<String> added, Collection<String> changed, Collection<String> removed) {
        super(oldContext, Arrays.asList(SUPPORTED), added, changed, removed);
        init(oldContext);
    }
    
    @Override
    protected void init(Context oldContext) {
        //1. Transitively flag removed children
        new HashSet<>(removed()).stream().flatMap(m -> m.getDescendants()).forEach(
                m -> add(Flag.DELETED, FlagCondition.OverrideFlag, m));
        
        //and dirty children
        new HashSet<>(dirty()).stream().flatMap(m -> m.getDescendants()).forEach(
                m -> add(new Flag(DIRTY, 1), FlagCondition.OverrideFlag, m));
        
        //2. Whenever there are added modules, flag their parent as unsure
        if (!added().isEmpty()) {
            add(new Flag(UNSURE, 1), FlagCondition.FlagIfClean, oldContext.getRootModule());
        }
        
        //3. Compute unsure = all modules that depend on dirty, removed or unsure modules or that have them as parent
        //I need to flag all modules that depend on the dirty modules (recursively) as possibly dirty (unsure)
        //Using a DFS algorithm with the reverse dependency edges in the graph
        Set<IModule> visited = new HashSet<>(dirty());
        visited.addAll(removed());
        visited.addAll(unsure());
        LinkedList<IModule> stack = new LinkedList<>(visited);
        while (!stack.isEmpty()) {
            IModule module = stack.pop();
            
            //Check modules that depend on this module
            for (String depModuleId : oldContext.getDependencies(module).getModuleDependantIds()) {
                IModule depModule = oldContext.getModuleUnchecked(depModuleId);
                if (depModule == null) {
                    TDebug.DEV_OUT.info("Dependent " + depModuleId + " of " + module.getId() + " does not exist");
                    continue; //This module no longer exists
                }
                if (!visited.add(depModule)) continue;
                if (depModule.getTopCleanliness() != CLEAN) TDebug.DEV_OUT.info("Cleanliness algorithm seems incorrect, encountered clean module " + depModule);

                add(new Flag(UNSURE, 1), FlagCondition.FlagIfClean, depModule);
                stack.push(depModule);
            }
            
            //Check child relations
            for (Scope scope : module.getScopeGraph().getParentScopes()) {
                IModule parent = Scopes.getOwnerUnchecked(oldContext, scope);
                if (!visited.add(parent)) continue;

                add(new Flag(UNSURE, 1), FlagCondition.FlagIfClean, parent);
                stack.push(parent);
            }
        }

        //#2 Compute clean = all modules that were not marked otherwise
        add(Flag.CLEAN, FlagCondition.DontFlag, oldContext.getModules().stream().filter(m -> m.getTopCleanliness() == CLEAN));

        if (TDebug.CHANGESET) TDebug.DEV_OUT.info("Based on the files, we identified:");
        if (TDebug.CHANGESET) TDebug.DEV_OUT.info("  Removed:  (" + removed().size()        + ") " + removedIds());
        if (TDebug.CHANGESET) TDebug.DEV_OUT.info("  Dirty:    (" + dirty().size()          + ") " + dirtyIds());
        if (TDebug.CHANGESET) TDebug.DEV_OUT.info("  Unsure:   (" + unsure().size()         + ") " + unsureIds());
        if (TDebug.CHANGESET) TDebug.DEV_OUT.info("  Clean:    (" + clean().size()          + ") " + cleanIds());
    }
}