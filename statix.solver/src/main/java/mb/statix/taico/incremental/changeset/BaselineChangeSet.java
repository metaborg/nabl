package mb.statix.taico.incremental.changeset;

import static mb.statix.taico.module.ModuleCleanliness.*;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;

import mb.statix.taico.incremental.Flag;
import mb.statix.taico.module.IModule;
import mb.statix.taico.module.ModuleCleanliness;
import mb.statix.taico.solver.SolverContext;

public class BaselineChangeSet extends AChangeSet2 {
    private static final long serialVersionUID = 1L;
    
    private static final ModuleCleanliness[] SUPPORTED = new ModuleCleanliness[] {
            CLEAN,
            CLIRTY,
            DELETED,
            DIRTY,
            CHILDOFDIRTY,
            NEW
    };
    
    public BaselineChangeSet(SolverContext oldContext,
            Collection<String> added, Collection<String> changed, Collection<String> removed) {
        super(oldContext, Arrays.asList(SUPPORTED), added, changed, removed);
        init(oldContext);
    }
    
    @Override
    protected void init(SolverContext oldContext) {
        //Mark all removed modules and descendants as deleted
        removed().stream().flatMap(m -> m.getDescendants()).forEach(m -> m.setFlagIfClean(Flag.DELETED));

        //#0 Compute child of dirty
        for (IModule module : dirty()) {
            add(new Flag(CHILDOFDIRTY, 1), FlagCondition.FlagIfClean, module.getDescendants());
        }

        //#1 Compute clirty = all modules that depend on dirty or clirty modules
        //I need to flag all modules that depend on the dirty modules (recursively) as possibly dirty (clirty)
        //Using a DFS algorithm with the reverse dependency edges in the graph
        Set<IModule> visited = new HashSet<>(dirty());
        visited.addAll(childOfDirty());
        visited.addAll(removed());
        LinkedList<IModule> stack = new LinkedList<>(visited);
        while (!stack.isEmpty()) {
            IModule module = stack.pop();
            for (IModule depModule : module.getDependants().keySet()) {
                if (visited.contains(depModule)) continue;
                if (depModule.getTopCleanliness() != CLEAN) System.err.println("Cleanliness algorithm seems incorrect, encountered ");
                visited.add(depModule);
                add(new Flag(CLIRTY, 1), FlagCondition.FlagIfClean, depModule);
                stack.push(depModule);
            }
        }
        
        //All modules that depend upon removed modules are also considered clirty.
        add(new Flag(CLIRTY, 1), FlagCondition.FlagIfClean, removed().stream().flatMap(m -> m.getDependants().keySet().stream()));

        //#2 Compute clean = all modules that were not marked otherwise
        add(Flag.CLEAN, FlagCondition.DontFlag, oldContext.getModules().stream().filter(m -> m.getTopCleanliness() == CLEAN));

        System.err.println("Based on the files, we identified:");
        System.err.println("  Dirty:  " + dirty().size()  + " modules (" + removed().size() + " removed)");
        System.err.println("  Clirty: " + clirty().size() + " modules");
        System.err.println("  Clean:  " + clean().size()  + " modules");
    }
}