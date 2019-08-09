package mb.statix.taico.incremental.changeset;

import static mb.statix.taico.module.ModuleCleanliness.*;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;

import com.google.common.collect.Sets;

import mb.statix.taico.incremental.Flag;
import mb.statix.taico.module.IModule;
import mb.statix.taico.module.ModuleCleanliness;
import mb.statix.taico.solver.Context;

public class NameChangeSet extends AChangeSet {
    private static final long serialVersionUID = 1L;
    
    private static final ModuleCleanliness[] SUPPORTED = new ModuleCleanliness[] {
            CLEAN,
            UNSURE,
            DELETED,
            DIRTY,
            NEW
    };
    
    public NameChangeSet(Context oldContext,
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
        
        //2. Flag the dependencies on Dirty and Removed modules as Unsure (level 1)
        for (IModule dirty : Sets.union(dirty(), removed())) {
            final int dirtyLevel = dirty.getTopLevel();
            final String dirtyId = dirty.getId();
            //TODO Store the queries in the reason?
            oldContext.getDependencies(dirtyId).getModuleDependants().stream().forEach(
                    m -> add(new Flag(UNSURE, dirtyLevel, dirtyId), FlagCondition.AddFlag, m));
        }
        
        //3. Flag Unsureness of higher levels (depends on unsure of lower levels).
        
        //I need to flag all modules that depend on the unsure modules with increasing levels of clirtiness
        //Using a BFS algorithm with the reverse dependency edges in the graph
        //This algorithm visits each module at most once.
        {
            LinkedList<IModule> queue = new LinkedList<>(unsure());
            while (!queue.isEmpty()) {
                IModule module = queue.removeFirst();
                final Flag moduleFlag = module.getTopFlag();
                //TODO Should this condition be removed?
                if (moduleFlag.getCleanliness() != UNSURE) continue;
                final int moduleLevel = moduleFlag.getLevel();
                final String moduleId = module.getId();
                
                for (IModule depModule : oldContext.getDependencies(module).getModuleDependants()) {
                    if (depModule == null) {
                        System.err.println("Encountered null depModule for " + module);
                        continue;
                    }
                    
                    Flag oldFlag = depModule.getTopFlag();
                    //If we already have a flag with the same cause, we don't want to visit this module again
                    if (!add(new Flag(UNSURE, moduleLevel + 1, moduleId), FlagCondition.FlagIfClean, depModule)) {
                        continue;
                    }
                    
                    //only if top flag changed do we need to revisit the module. Otherwise it does not make a difference for dependants
                    if (oldFlag != depModule.getTopFlag()) queue.addLast(depModule);
                }
            }
        }
        
        //3. Flag all the remaining modules as clean.
        add(Flag.CLEAN, FlagCondition.DontFlag, oldContext.getModules().stream().filter(m -> m.getTopCleanliness() == CLEAN));

        System.err.println("Based on the files, we identified:");
        System.err.println("  Removed:  (" + removed().size()        + ") " + removedIds());
        System.err.println("  Dirty:    (" + dirty().size()          + ") " + dirtyIds());
        System.err.println("  Unsure:   (" + unsure().size()         + ") " + unsureIds());
        System.err.println("  Clean:    (" + clean().size()          + ") " + cleanIds());
        
    }
}
