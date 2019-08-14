package mb.statix.modular.incremental.strategy;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import mb.statix.modular.incremental.changeset.BaselineChangeSet;
import mb.statix.modular.incremental.changeset.IChangeSet;
import mb.statix.modular.module.IModule;
import mb.statix.modular.module.ModulePaths;
import mb.statix.modular.solver.Context;
import mb.statix.solver.IConstraint;

public class NonIncrementalStrategy extends IncrementalStrategy {
    private static final long serialVersionUID = 1L;
    
    @Override
    public IChangeSet createChangeSet(Context oldContext, Collection<String> added, Collection<String> changed,
            Collection<String> removed) {
        //TODO Is this correct? Does this not allow some module access already?
        return new BaselineChangeSet(oldContext, added, changed, removed);
    }
    
    @Override
    public IModule getModule(Context context, Context oldContext, String requester, String id) {
        return context.getModuleManager().getModule(id);
    }
    
    @Override
    public IModule getChildModule(Context context, Context oldContext, IModule requester, String childId) {
        //Always allow child access when we are not being incremental
        return context.getModuleManager().getModule(childId);
    }
    
    @Override
    public Map<IModule, IConstraint> createInitialModules(Context context,
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
