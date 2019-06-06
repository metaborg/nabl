package mb.statix.taico.incremental.changeset;

import static mb.statix.taico.module.ModuleCleanliness.*;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;

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
            NEW
    };
    
    public QueryChangeSet(SolverContext oldContext,
            Collection<String> added, Collection<String> changed, Collection<String> removed) {
        super(oldContext, Arrays.asList(SUPPORTED), added, changed, removed);
        init(oldContext);
    }
    
    @Override
    protected void init(SolverContext oldContext) {
        //Dirty, new and removed are already flagged.
        
        //1. Transitively flag removed children
        new HashSet<>(removed()).stream().flatMap(m -> m.getDescendants()).forEach(
                m -> add(Flag.DELETED, FlagCondition.OverrideFlag, m));
        
        //and dirty children
        new HashSet<>(dirty()).stream().flatMap(m -> m.getDescendants()).forEach(
                m -> add(new Flag(DIRTY, 1), FlagCondition.OverrideFlag, m));
        
        //2. Flag the parents of dirty modules as DirtyChild if they pass a scope owned by them to the child
        for (IModule dirty : dirty()) {
            final int dirtyLevel = dirty.getTopLevel();
            final String dirtyId = dirty.getId();
            
            for (Scope scope : dirty.getScopeGraph().getParentScopes()) {
                IModule parent = Scopes.getOwnerUnchecked(oldContext, scope);
                add(new Flag(DIRTYCHILD, dirtyLevel, dirtyId), FlagCondition.AddFlag, parent);
            }
        }
        
        //4. Flag the dependencies on dirty or removed modules as Clirty and ClirtyChild (level 1)
        for (IModule dirty : Sets.union(dirty(), removed())) {
            final int dirtyLevel = dirty.getTopLevel();
            final String dirtyId = dirty.getId();
            //TODO Store the queries in the reason?
            dirty.getDependants().keySet().stream().forEach(
                    m -> add(new Flag(CLIRTY, dirtyLevel, dirtyId), FlagCondition.AddFlag, m));
        }
        
        for (IModule dirtyChild : hasDirtyChild()) {
            final int dirtyLevel = dirtyChild.getTopLevel();
            final String dirtyId = dirtyChild.getId();
            //TODO Store the queries in the reason?
            dirtyChild.getDependants().keySet().stream().forEach(
                    m -> add(new Flag(CLIRTY, dirtyLevel, dirtyId), FlagCondition.AddFlag, m));
        }
        
        //... and then continue by doing this for Clirtyness of higher levels.
        
        //I need to flag all modules that depend on the clirty modules with increasing levels of clirtiness
        //Using a DFS algorithm with the reverse dependency edges in the graph
        //TODO With cycles in the dependency graph, this will trigger an infinite process of flagging as higher levels of clirty.
        //We only want each module to be flagged as clirty 
        {
            Set<IModule> visited = new HashSet<>(clirty());
            visited.addAll(hasClirtyChild());
            LinkedList<IModule> stack = new LinkedList<>(visited);
            while (!stack.isEmpty()) {
                IModule module = stack.pop();
                final Flag moduleFlag = module.getTopFlag();
                if (moduleFlag.getCleanliness() != CLIRTY) continue;
                final int moduleLevel = moduleFlag.getLevel();
                final String moduleId = module.getId();
                
                for (IModule depModule : module.getDependants().keySet()) {
                    if (visited.contains(depModule)) continue;
                    
                    add(new Flag(CLIRTY, moduleLevel + 1, moduleId), FlagCondition.AddFlagIfNotSameCause, depModule);
                    if (visited.add(depModule)) stack.push(depModule); //Only add to the stack if we haven't visited it before
                }
            }
        }
        
        //5. Flag all the remaining modules as clean.
        add(Flag.CLEAN, FlagCondition.DontFlag, oldContext.getModules().stream().filter(m -> m.getTopCleanliness() == CLEAN));

        System.err.println("Based on the files, we identified:");
        System.err.println("  Dirty:   (" + dirtyIds().size()   + ") " + dirtyIds());
        System.err.println("  Removed: (" + removedIds().size() + ") " + removedIds());
        System.err.println("  Clirty:  (" + clirtyIds().size()  + ") " + clirtyIds());
        System.err.println("  Clean:   (" + cleanIds().size()   + ") " + cleanIds());
    }
}
