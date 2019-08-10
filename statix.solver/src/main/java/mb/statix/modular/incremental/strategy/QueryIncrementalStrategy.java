package mb.statix.modular.incremental.strategy;

import java.util.Collection;

import mb.statix.modular.incremental.changeset.IChangeSet;
import mb.statix.modular.incremental.changeset.QueryChangeSet;
import mb.statix.modular.incremental.manager.QueryIncrementalManager;
import mb.statix.modular.module.IModule;
import mb.statix.modular.scopegraph.reference.ModuleDelayException;
import mb.statix.modular.solver.Context;

public class QueryIncrementalStrategy extends IncrementalStrategy {
    
    @Override
    public QueryIncrementalManager createManager() {
        return new QueryIncrementalManager();
    }
    
    @Override
    public IChangeSet createChangeSet(Context oldContext, Collection<String> added, Collection<String> changed,
            Collection<String> removed) {
        return new QueryChangeSet(oldContext, added, changed, removed);
    }
    
    @Override
    public IModule getModule(Context context, Context oldContext, String requesterId, String id) throws ModuleDelayException {
        //TODO Move this method to the incremental manager
        
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
    public IModule getChildModule(Context context, Context oldContext, IModule requester, String childId) {
        //Child access works the same as normal access.
        return getModule(context, oldContext, requester.getId(), childId);
    }
}
