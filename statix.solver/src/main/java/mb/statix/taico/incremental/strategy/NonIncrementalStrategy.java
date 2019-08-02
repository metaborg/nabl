package mb.statix.taico.incremental.strategy;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import mb.statix.solver.IConstraint;
import mb.statix.taico.incremental.changeset.BaselineChangeSet;
import mb.statix.taico.incremental.changeset.IChangeSet;
import mb.statix.taico.module.IModule;
import mb.statix.taico.module.ModulePaths;
import mb.statix.taico.solver.SolverContext;

public class NonIncrementalStrategy extends IncrementalStrategy {
    
    @Override
    public IChangeSet createChangeSet(SolverContext oldContext, Collection<String> added, Collection<String> changed,
            Collection<String> removed) {
        //TODO Is this correct? Does this not allow some module access already?
        return new BaselineChangeSet(oldContext, added, changed, removed);
    }
    
    @Override
    public IModule getModule(SolverContext context, SolverContext oldContext, String requester, String id) {
        return context.getModuleManager().getModule(id);
    }
    
    @Override
    public IModule getChildModule(SolverContext context, SolverContext oldContext, IModule requester, String childId) {
        //Always allow child access when we are not being incremental
        return context.getModuleManager().getModule(childId);
    }
    
    @Override
    public Map<IModule, IConstraint> createInitialModules(SolverContext context,
            IChangeSet changeSet,
            Map<String, IConstraint> moduleConstraints) {
        Map<IModule, IConstraint> newModules = new HashMap<>();
        for (Entry<String, IConstraint> entry : moduleConstraints.entrySet()) {
            final String nameOrId = entry.getKey();
            //Skip entries that are not top level
            if (ModulePaths.pathLength(nameOrId) > 2) {
                System.err.println("Changeset has larger precision than expected by the non incremental strategy. "
                        + "Skipping entry " + nameOrId + " because only top level modules are supported.");
                continue;
            }
            
            String name = ModulePaths.getName(nameOrId);
            IModule module = createFileModule(context, name, entry.getValue(), null);
            newModules.put(module, entry.getValue());
        }
        
        return newModules;
    }
}
