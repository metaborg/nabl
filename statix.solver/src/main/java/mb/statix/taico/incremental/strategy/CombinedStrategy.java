package mb.statix.taico.incremental.strategy;

import java.util.Collection;
import java.util.List;

import org.checkerframework.checker.nullness.qual.Nullable;

import mb.statix.scopegraph.terms.Scope;
import mb.statix.solver.IConstraint;
import mb.statix.taico.incremental.changeset.CombinedChangeSet;
import mb.statix.taico.incremental.changeset.IChangeSet;
import mb.statix.taico.incremental.manager.CombinedIncrementalManager;
import mb.statix.taico.module.IModule;
import mb.statix.taico.module.ModuleCleanliness;
import mb.statix.taico.scopegraph.reference.ModuleDelayException;
import mb.statix.taico.solver.Context;

public class CombinedStrategy extends IncrementalStrategy {
    
    @Override
    public CombinedIncrementalManager createManager() {
        return new CombinedIncrementalManager();
    }
    
    @Override
    public IChangeSet createChangeSet(Context oldContext, Collection<String> added, Collection<String> changed,
            Collection<String> removed) {
        return new CombinedChangeSet(oldContext, added, changed, removed);
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
    protected IModule createFileModule(
            Context context, String childName, IConstraint initConstraint, @Nullable IModule oldModule) {
        boolean transferDeps = false;
        if (oldModule != null && oldModule.getTopCleanliness() == ModuleCleanliness.CLEAN) {
            transferDeps = true;
        }
        
        System.err.println("[IS] Creating file module for " + childName);

        List<Scope> scopes = getScopes(initConstraint);
        
        IModule rootOwner = context.getRootModule();
        IModule child = rootOwner.createChild(childName, scopes, initConstraint, transferDeps);
        rootOwner.addChild(child);
        return child;
    }
}

