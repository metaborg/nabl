package mb.statix.taico.incremental.strategy;

import java.util.Collection;

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
    
    @Override
    public IModule getModule(Context context, Context oldContext, String requesterId, String id) throws ModuleDelayException {
        //TODO Move this method to the incremental manager
        
        IModule module = context.getModuleManager().getModule(id);
        if (module != null) return module;

        if (oldContext == null) return null;
        module = oldContext.getModuleManager().getModule(id);
        if (module == null) return null;
        
        //TODO Move to IncrementalManager?
//        if (!context.<NameIncrementalManager>getIncrementalManager().isAllowedAccess(id)) {
//            throw new ModuleDelayException(id);
//        }
        
        return module;
    }
    
    @Override
    public IModule getChildModule(Context context, Context oldContext, IModule requester, String childId) {
        //Child access works the same as normal access.
        return getModule(context, oldContext, requester.getId(), childId);
    }
}
