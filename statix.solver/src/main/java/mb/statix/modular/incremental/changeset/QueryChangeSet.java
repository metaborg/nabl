package mb.statix.modular.incremental.changeset;

import static mb.statix.modular.module.ModuleCleanliness.*;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;

import com.google.common.collect.Sets;

import mb.statix.modular.incremental.Flag;
import mb.statix.modular.module.IModule;
import mb.statix.modular.module.ModuleCleanliness;
import mb.statix.modular.solver.Context;
import mb.statix.modular.util.Scopes;
import mb.statix.modular.util.TDebug;
import mb.statix.scopegraph.terms.Scope;

public class QueryChangeSet extends AChangeSet {
    private static final long serialVersionUID = 1L;
    
    private static final ModuleCleanliness[] SUPPORTED = new ModuleCleanliness[] {
            CLEAN,
            UNSURE,
            UNSURECHILD,
            DELETED,
            DIRTY,
            DIRTYCHILD,
            NEW,
            NEWCHILD
    };
    
    public QueryChangeSet(Context oldContext,
            Collection<String> added, Collection<String> changed, Collection<String> removed) {
        super(oldContext, Arrays.asList(SUPPORTED), added, changed, removed);
        init(oldContext);
    }
    
    @Override
    protected void init(Context oldContext) {
        //Dirty, new and removed are already flagged.
        
        //0. Flag newchild
        if (!added().isEmpty()) {
            add(Flag.NEWCHILD, FlagCondition.OverrideFlag, oldContext.getRootModule());
        }
        
        //1. Transitively flag removed children
        new HashSet<>(removed()).stream().flatMap(m -> m.getDescendants()).forEach(
                m -> add(Flag.DELETED, FlagCondition.OverrideFlag, m));
        
        //and dirty children
        new HashSet<>(dirty()).stream().flatMap(m -> m.getDescendants()).forEach(
                m -> add(new Flag(DIRTY, 1), FlagCondition.OverrideFlag, m));
        
        //2. Flag the parents of dirty modules as DirtyChild if they pass a scope owned by them to the child, with the same level
        for (IModule dirty : dirty()) {
            final int dirtyLevel = dirty.getTopLevel();
            final String dirtyId = dirty.getId();
            
            for (Scope scope : dirty.getScopeGraph().getParentScopes()) {
                IModule parent = Scopes.getOwnerUnchecked(oldContext, scope);
                add(new Flag(DIRTYCHILD, dirtyLevel, dirtyId), FlagCondition.AddFlag, parent);
            }
        }
        
        //TODO IMPORTANT 3. Flag the parents of removed modules as RemovedChild if they pass a scope owned by them to the child, with the same level.
        //(Only if the parent does not have the removed flag already)
        
        
        //4. Flag the dependencies on Dirty and Removed modules as Unsure (level 1)
        for (IModule dirty : Sets.union(dirty(), removed())) {
            final int dirtyLevel = dirty.getTopLevel();
            final String dirtyId = dirty.getId();
            //TODO Store the queries in the reason?
            oldContext.getDependencies(dirtyId).getModuleDependants().stream().forEach(
                    m -> add(new Flag(UNSURE, dirtyLevel, dirtyId), FlagCondition.AddFlag, m));
        }
        
        //5. Flag the dependencies on DirtyChild and NewChild as Unsure (level 2)
        for (IModule dirtyChild : Sets.union(hasDirtyChild(), hasNewChild())) {
            final int dirtyLevel = dirtyChild.getTopLevel();
            final String dirtyId = dirtyChild.getId();
            //TODO Store the queries in the reason?
            oldContext.getDependencies(dirtyId).getModuleDependants().stream().forEach(
                    m -> add(new Flag(UNSURE, dirtyLevel + 1, dirtyId), FlagCondition.AddFlag, m));
        }
        
        //6. Flag Unsureness of higher levels (depends on unsure of lower levels).
        
        //I need to flag all modules that depend on the unsure modules with increasing levels of clirtiness
        //Using a BFS algorithm with the reverse dependency edges in the graph
        //This algorithm visits each module at most once.
        {
            LinkedList<IModule> queue = new LinkedList<>(unsure());
            while (!queue.isEmpty()) {
                IModule module = queue.removeFirst();
                final Flag moduleFlag = module.getTopFlag();
                //TODO Should this condition be removed?
                if (moduleFlag.getCleanliness() != UNSURE && moduleFlag.getCleanliness() != UNSURECHILD) continue;
                final int moduleLevel = moduleFlag.getLevel();
                final String moduleId = module.getId();
                
                for (IModule depModule : oldContext.getDependencies(moduleId).getModuleDependants()) {
                    if (depModule == null) {
                        TDebug.DEV_OUT.info("Encountered null depModule for " + module);
                        continue;
                    }
                    
                    Flag oldFlag = depModule.getTopFlag();
                    //If we already have a flag with the same cause, we don't want to visit this module again
                    if (!add(new Flag(UNSURE, moduleLevel + 1, moduleId), FlagCondition.AddFlagIfNotSameCause, depModule)) {
                        continue;
                    }
                    
                    //only if top flag changed do we need to revisit the module. Otherwise it does not make a difference for dependants
                    if (oldFlag != depModule.getTopFlag()) queue.addLast(depModule);
                }
                
                //Add the unsurechild flag AFTER adding the unsure flags.
                for (Scope scope : module.getScopeGraph().getParentScopes()) {
                    IModule parent = Scopes.getOwnerUnchecked(oldContext, scope);
                    Flag oldFlag = parent.getTopFlag();
                    if (!add(new Flag(UNSURECHILD, moduleLevel + 1, moduleId), FlagCondition.AddFlagIfNotSameCause, parent)) {
                        continue;
                    }
                    
                    //only if top flag changed do we need to revisit the module. Otherwise it does not make a difference for dependants
                    if (oldFlag != parent.getTopFlag()) queue.addLast(parent);
                }
            }
        }
        
        //7. Flag all the remaining modules as clean.
        add(Flag.CLEAN, FlagCondition.DontFlag, oldContext.getModules().stream().filter(m -> m.getTopCleanliness() == CLEAN));

        if (TDebug.CHANGESET) TDebug.DEV_OUT.info("Based on the files, we identified:");
        if (TDebug.CHANGESET) TDebug.DEV_OUT.info("  Removed:  (" + removed().size()        + ") " + removedIds());
        if (TDebug.CHANGESET) TDebug.DEV_OUT.info("  NewChld:  (" + hasNewChild().size()    + ") " + hasNewChildIds());
        if (TDebug.CHANGESET) TDebug.DEV_OUT.info("  Dirty:    (" + dirty().size()          + ") " + dirtyIds());
        if (TDebug.CHANGESET) TDebug.DEV_OUT.info("  DirtyCh:  (" + hasDirtyChild().size()  + ") " + hasDirtyChildIds());
        if (TDebug.CHANGESET) TDebug.DEV_OUT.info("  Unsure:   (" + unsure().size()         + ") " + unsureIds());
        if (TDebug.CHANGESET) TDebug.DEV_OUT.info("  UnsureCh: (" + hasUnsureChild().size() + ") " + hasUnsureChildIds());
        if (TDebug.CHANGESET) TDebug.DEV_OUT.info("  Clean:    (" + clean().size()          + ") " + cleanIds());
        
    }
}
