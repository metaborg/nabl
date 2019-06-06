package mb.statix.taico.incremental.strategy;

import static mb.statix.taico.module.ModuleCleanliness.*;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.google.common.collect.Sets;

import mb.statix.scopegraph.terms.Scope;
import mb.statix.solver.IConstraint;
import mb.statix.solver.ISolverResult;
import mb.statix.solver.log.IDebugContext;
import mb.statix.taico.incremental.Flag;
import mb.statix.taico.incremental.changeset.AChangeSet2;
import mb.statix.taico.incremental.changeset.IChangeSet;
import mb.statix.taico.incremental.changeset.IChangeSet2;
import mb.statix.taico.incremental.manager.IncrementalManager;
import mb.statix.taico.module.IModule;
import mb.statix.taico.module.ModuleCleanliness;
import mb.statix.taico.scopegraph.reference.ModuleDelayException;
import mb.statix.taico.solver.IMState;
import mb.statix.taico.solver.SolverContext;
import mb.statix.taico.util.Scopes;

public class QueryIncrementalStrategy extends IncrementalStrategy {
    /**
     * Reanalyzes the modules that are not marked as clean.
     */
    @Override
    public Map<String, ISolverResult> reanalyze(IChangeSet changeSet, IMState baseState, Map<String, IConstraint> constraints, IDebugContext debug) throws InterruptedException {
        return baseState.coordinator().solve(this, changeSet, baseState, constraints, debug);
    }
    
    @Override
    public IChangeSet2 createChangeSet(SolverContext oldContext, Collection<String> added, Collection<String> changed,
            Collection<String> removed) {
        return new QueryChangeSet(oldContext, added, changed, removed);
    }
    
    @Override
    public IModule getModule(SolverContext context, SolverContext oldContext, IModule requester, String id) throws ModuleDelayException {
        IModule module = context.getModuleManager().getModule(id);
        if (module != null) return module;

        if (oldContext == null) return null;
        module = oldContext.getModuleManager().getModule(id);
        if (module == null) return null;
        
        //Depending on the current phase, we want to do something different
        final Flag moduleFlag = module.getTopFlag();
        final ModuleCleanliness cleanliness = moduleFlag.getCleanliness();
        final int level = moduleFlag.getLevel();
        
        switch (context.<QIPhase>getPhase()) {
            case Dirty:
                
            case Final:
                System.err.println("Final phase, allowing module access to " + module + " (" + module.getTopCleanliness() + ")");
                return module;
        }
        
        if (module.getFlag() == ModuleCleanliness.CLEAN) return module;
        
        return null;
    }
    
    @Override
    public IModule getChildModule(SolverContext context, SolverContext oldContext, IModule requester, String childId) {
        //Child access works the same as normal access.
        return getModule(context, oldContext, requester, childId);
    }
    
    @Override
    public Map<IModule, IConstraint> createModulesForPhase(SolverContext context,
            IChangeSet changeSet,
            Map<String, IConstraint> moduleConstraints) {
        Map<IModule, IConstraint> newModules = new HashMap<>();
        for (Entry<String, IConstraint> entry : moduleConstraints.entrySet()) {
            System.err.println("[QI] Encountered entry for " + entry.getKey());
            IModule oldModule = context.getOldContext().map(c -> c.getModuleByName(entry.getKey(), 1)).orElse(null);
            
            if (oldModule == null || oldModule.getTopCleanliness() != ModuleCleanliness.CLEAN) {
                IModule module = createFileModule(context, entry.getKey(), entry.getValue());
                newModules.put(module, entry.getValue());
            } else {
                //Old module is clean, we can reuse it
                reuseOldModule(context, changeSet, oldModule);
            }
        }
        
        return newModules;
    }
    
    @Override
    protected void reuseOldModule(SolverContext context, IChangeSet changeSet, IModule oldModule) {
        IModule newModule = oldModule.copy();
        for (IModule child : changeSet.removed()) {
            newModule.getScopeGraph().removeChild(child);
        }
        context.addModule(newModule);
        super.reuseOldModule(context, changeSet, newModule);
    }
    
    @Override
    public boolean endOfPhase(SolverContext context) {
        return false;
    }
    
    public static class QueryChangeSet extends AChangeSet2 {
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
    
    public static enum QIPhase {
        Dirty,
        
        
        /** The final phase where we just do normal solving. */
        Final
        
    }
    
    public static class QueryIncrementalManager extends IncrementalManager {
        
    }
}
