package mb.statix.taico.incremental.strategy;

import static mb.statix.taico.module.ModuleCleanliness.CLEAN;

import java.util.Collection;
import java.util.List;

import org.checkerframework.checker.nullness.qual.Nullable;

import mb.statix.scopegraph.terms.Scope;
import mb.statix.solver.IConstraint;
import mb.statix.taico.incremental.changeset.BaselineChangeSet;
import mb.statix.taico.incremental.changeset.IChangeSet;
import mb.statix.taico.module.IModule;
import mb.statix.taico.module.ModuleCleanliness;
import mb.statix.taico.solver.Context;

/**
 * Incremental strategy which is naive and simply redoes all modules that have changed and all
 * modules that depend on them (transitively) (dirty U clirty).
 * 
 * In other words, only modules that are unchanged and that do not transitively depend on modules
 * which are changed, are redone.
 * 
 * However, if any modules are added, we cannot guarantee that they don't affect existing modules,
 * so we redo all modules if modules are added.
 */
public class BaselineIncrementalStrategy extends IncrementalStrategy {
//    @Override
//    public void clearDirtyModules(IChangeSet changeSet, ModuleManager manager) {
//        if (!changeSet.added().isEmpty()) {
//            //We cannot guarantee that these additions don't influence existing results, so we will flag everything as dirty (except for the top module)
//            manager.retainModules(manager.topLevelModules());
//            return;
//        }
//        
//        //TODO: redo dirty and clirty, with clean in the context
//        Set<IModule> redo = Sets.union(changeSet.dirty(), changeSet.clirty());
//        if (redo.isEmpty()) return;
//
//        //Delete all modules that need to be redone as well as their children
//        for (IModule module : redo) {
//            //Invariant: global scope never changes
//            if (ModulePaths.pathSegments(module.getId(), 2).length == 1) {
//                throw new IllegalStateException("Top module is in the set to redo. This should not be correct!");
//            }
//            
//            manager.purgeModules(module);
//        }
//    }
    
    @Override
    public IChangeSet createChangeSet(Context oldContext, Collection<String> added, Collection<String> changed,
            Collection<String> removed) {
        return new BaselineChangeSet(oldContext, added, changed, removed);
    }
    
    @Override
    public IModule getModule(Context context, Context oldContext, String requesterId, String id) {
        IModule module = context.getModuleManager().getModule(id);
        if (module != null) return module;
        
        //We need to redo everything if a file was added, otherwise we are missing dependencies.
        //TODO Instead, with the correct dependencies, we would not get dependencies on the individual children, but rather on their parent. This would mean that additions do work properly.
        if (oldContext == null || !context.getChangeSet().added().isEmpty()) return null;
        module = oldContext.getModuleManager().getModule(id);
        
        if (module == null) return null;
        if (module.getTopCleanliness() == CLEAN) return module;
        
        return null;
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
