package mb.statix.taico.incremental.changeset;

import static mb.statix.taico.module.ModuleCleanliness.*;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;

import com.google.common.collect.Sets;

import mb.statix.scopegraph.terms.Scope;
import mb.statix.taico.incremental.Flag;
import mb.statix.taico.module.IModule;
import mb.statix.taico.module.ModuleCleanliness;
import mb.statix.taico.solver.SolverContext;
import mb.statix.taico.util.Scopes;

public class QueryChangeSet extends AChangeSet2 {
    private static final long serialVersionUID = 1L;
    
    private static final ModuleCleanliness[] SUPPORTED = new ModuleCleanliness[] {
            CLEAN,
            CLIRTY,
            CLIRTYCHILD,
            DELETED,
            DIRTY,
            DIRTYCHILD,
            NEW,
            NEWCHILD
    };
    
    public QueryChangeSet(SolverContext oldContext,
            Collection<String> added, Collection<String> changed, Collection<String> removed) {
        super(oldContext, Arrays.asList(SUPPORTED), added, changed, removed);
        init(oldContext);
    }
    
    @Override
    protected void init(SolverContext oldContext) {
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
        
        //4. Flag the dependencies on Dirty and Removed modules as Clirty (level 1)
        for (IModule dirty : Sets.union(dirty(), removed())) {
            final int dirtyLevel = dirty.getTopLevel();
            final String dirtyId = dirty.getId();
            //TODO Store the queries in the reason?
            dirty.getDependants().keySet().stream().forEach(
                    m -> add(new Flag(CLIRTY, dirtyLevel, dirtyId), FlagCondition.AddFlag, m));
        }
        
        //5. Flag the dependencies on DirtyChild and NewChild as Clirty (level 2)
        for (IModule dirtyChild : Sets.union(hasDirtyChild(), hasNewChild())) {
            final int dirtyLevel = dirtyChild.getTopLevel();
            final String dirtyId = dirtyChild.getId();
            //TODO Store the queries in the reason?
            dirtyChild.getDependants().keySet().stream().forEach(
                    m -> add(new Flag(CLIRTY, dirtyLevel + 1, dirtyId), FlagCondition.AddFlag, m));
        }
        
        //6. Flag Clirtyness of higher levels (depends on clirty of lower levels).
        
        //I need to flag all modules that depend on the clirty modules with increasing levels of clirtiness
        //Using a DFS algorithm with the reverse dependency edges in the graph
        //TODO With cycles in the dependency graph, this will trigger an infinite process of flagging as higher levels of clirty.
        //We only want each module to be flagged as clirty 
        
        {
            LinkedList<IModule> stack = new LinkedList<>(clirty());
            while (!stack.isEmpty()) {
                IModule module = stack.pop();
                final Flag moduleFlag = module.getTopFlag();
                if (moduleFlag.getCleanliness() != CLIRTY && moduleFlag.getCleanliness() != CLIRTYCHILD) continue;
                final int moduleLevel = moduleFlag.getLevel();
                final String moduleId = module.getId();
                
                for (IModule depModule : module.getDependants().keySet()) {
                    Flag oldFlag = depModule.getTopFlag();
                    //If we already have a flag with the same cause, we don't want to visit this module again
                    if (!add(new Flag(CLIRTY, moduleLevel + 1, moduleId), FlagCondition.AddFlagIfNotSameCause, depModule)) {
                        continue;
                    }
                    
                    //only if top flag changed do we need to revisit the module. Otherwise it does not make a difference for dependants
                    if (oldFlag != depModule.getTopFlag()) stack.push(depModule);
                }
                
                //Add the clirtychild flag AFTER adding the clirty flags.
                for (Scope scope : module.getScopeGraph().getParentScopes()) {
                    IModule parent = Scopes.getOwnerUnchecked(oldContext, scope);
                    Flag oldFlag = parent.getTopFlag();
                    if (!add(new Flag(CLIRTYCHILD, moduleLevel + 1, moduleId), FlagCondition.AddFlagIfNotSameCause, parent)) {
                        continue;
                    }
                    
                    //only if top flag changed do we need to revisit the module. Otherwise it does not make a difference for dependants
                    if (oldFlag != parent.getTopFlag()) stack.push(parent);
                }
            }
        }
        
        //7. Flag all the remaining modules as clean.
        add(Flag.CLEAN, FlagCondition.DontFlag, oldContext.getModules().stream().filter(m -> m.getTopCleanliness() == CLEAN));

        System.err.println("Based on the files, we identified:");
        System.err.println("  Removed:  (" + removed().size()        + ") " + removedIds());
        System.err.println("  NewChld:  (" + hasNewChild().size()    + ") " + hasNewChildIds());
        System.err.println("  Dirty:    (" + dirty().size()          + ") " + dirtyIds());
        System.err.println("  DirtyCh:  (" + hasDirtyChild().size()  + ") " + hasDirtyChildIds());
        System.err.println("  Clirty:   (" + clirty().size()         + ") " + clirtyIds());
        System.err.println("  ClirtyCh: (" + hasClirtyChild().size() + ") " + hasClirtyChildIds());
        System.err.println("  Clean:    (" + clean().size()          + ") " + cleanIds());
        
    }
}