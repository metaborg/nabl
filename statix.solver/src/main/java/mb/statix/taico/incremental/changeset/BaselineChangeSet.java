package mb.statix.taico.incremental.changeset;

import static mb.statix.taico.module.ModuleCleanliness.*;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;

import mb.statix.scopegraph.terms.Scope;
import mb.statix.taico.incremental.Flag;
import mb.statix.taico.module.IModule;
import mb.statix.taico.module.ModuleCleanliness;
import mb.statix.taico.solver.SolverContext;
import mb.statix.taico.util.Scopes;

public class BaselineChangeSet extends AChangeSet2 {
    private static final long serialVersionUID = 1L;
    
    private static final ModuleCleanliness[] SUPPORTED = new ModuleCleanliness[] {
            CLEAN,
            CLIRTY,
            DELETED,
            DIRTY,
            NEW
    };
    
    public BaselineChangeSet(SolverContext oldContext,
            Collection<String> added, Collection<String> changed, Collection<String> removed) {
        super(oldContext, Arrays.asList(SUPPORTED), added, changed, removed);
        init(oldContext);
    }
    
    @Override
    protected void init(SolverContext oldContext) {
        //1. Transitively flag removed children
        new HashSet<>(removed()).stream().flatMap(m -> m.getDescendants()).forEach(
                m -> add(Flag.DELETED, FlagCondition.OverrideFlag, m));
        
        //and dirty children
        new HashSet<>(dirty()).stream().flatMap(m -> m.getDescendants()).forEach(
                m -> add(new Flag(DIRTY, 1), FlagCondition.OverrideFlag, m));
        
        //2. Whenever there are added modules, flag their parent as clirty
        if (!added().isEmpty()) {
            add(new Flag(CLIRTY, 1), FlagCondition.FlagIfClean, oldContext.getRootModule());
        }
        
        //3. Compute clirty = all modules that depend on dirty, removed or clirty modules or that have them as parent
        //I need to flag all modules that depend on the dirty modules (recursively) as possibly dirty (clirty)
        //Using a DFS algorithm with the reverse dependency edges in the graph
        Set<IModule> visited = new HashSet<>(dirty());
        visited.addAll(removed());
        visited.addAll(clirty());
        LinkedList<IModule> stack = new LinkedList<>(visited);
        while (!stack.isEmpty()) {
            IModule module = stack.pop();
            
            //Check modules that depend on this module
            for (IModule depModule : module.getDependants().keySet()) {
                if (!visited.add(depModule)) continue;
                if (depModule.getTopCleanliness() != CLEAN) System.err.println("Cleanliness algorithm seems incorrect, encountered clean module " + depModule);

                add(new Flag(CLIRTY, 1), FlagCondition.FlagIfClean, depModule);
                stack.push(depModule);
            }
            
            //Check child relations
            for (Scope scope : module.getScopeGraph().getParentScopes()) {
                IModule parent = Scopes.getOwnerUnchecked(oldContext, scope);
                if (!visited.add(parent)) continue;

                add(new Flag(CLIRTY, 1), FlagCondition.FlagIfClean, parent);
                stack.push(parent);
            }
        }

        //#2 Compute clean = all modules that were not marked otherwise
        add(Flag.CLEAN, FlagCondition.DontFlag, oldContext.getModules().stream().filter(m -> m.getTopCleanliness() == CLEAN));

        System.err.println("Based on the files, we identified:");
        System.err.println("  Dirty:  " + dirty().size()  + " modules (" + removed().size() + " removed)");
        System.err.println("  Clirty: " + clirty().size() + " modules");
        System.err.println("  Clean:  " + clean().size()  + " modules");
    }
}