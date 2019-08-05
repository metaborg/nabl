package mb.statix.taico.incremental.strategy;

import java.io.Serializable;
import java.util.Collection;
import java.util.function.Function;

import mb.statix.taico.dependencies.DependencyManager;
import mb.statix.taico.dependencies.NameDependencies;
import mb.statix.taico.incremental.changeset.IChangeSet;
import mb.statix.taico.incremental.changeset.NameChangeSet;
import mb.statix.taico.incremental.manager.NameIncrementalManager;
import mb.statix.taico.module.IModule;
import mb.statix.taico.scopegraph.reference.ModuleDelayException;
import mb.statix.taico.solver.Context;

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
}

