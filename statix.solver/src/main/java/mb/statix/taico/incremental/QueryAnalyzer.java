package mb.statix.taico.incremental;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import mb.nabl2.terms.ITerm;
import mb.statix.scopegraph.terms.Scope;
import mb.statix.solver.IConstraint;
import mb.statix.taico.incremental.changeset.IChangeSet2;
import mb.statix.taico.incremental.manager.QueryIncrementalManager;
import mb.statix.taico.module.IModule;
import mb.statix.taico.scopegraph.reference.TrackingNameResolution;
import mb.statix.taico.solver.IMState;
import mb.statix.taico.solver.SolverContext;

public class QueryAnalyzer {
    private IChangeSet2 changeSet;
    private Map<IModule, Set<IConstraint>> leftConstraints = new HashMap<>();
    
    public void phase1() {
        Set<IModule> dirty = changeSet.dirty();
        
        //Now we need to solve dirty modules again.
        for (IModule module : dirty) {
            SolverContext.context().addModule(module);
            
            //Add the initialization constraint as incomplete
            IMState state = module.getCurrentState();
            state.solver().getCompleteness().add(module.getInitialization(), state.unifier());
        }
        
        //We need to add the init constraints to the completeness
        
    }
    
    public void checkQueries(IModule module) {
        //TODO We need to know based on the
        
        Flag flag = module.getTopFlag();
        if (flag.getCleanliness().isCleanish()) {
            //This module is clean.
            switchToClean(module);
        }
        module.queries();
        //The given module should be a clirty one. We now have to redo it's queries
        //TrackingNameResolution<Scope, ITerm, ITerm> nameResolution = TrackingNameResolution.builder();
    }
    
    public void switchToClean(IModule module) {
        //Set as clean
        module.setFlag(Flag.CLEAN);
        
        //Allow module access
        SolverContext.context().<QueryIncrementalManager>getIncrementalManager().allowAccess(module.getId());
        
        //Resolve pending incompleteness
        IMState state = module.getCurrentState();
        Set<IConstraint> pending = state.solver().getStore().getAllRemainingConstraints();
        //The pending set should be empty
        assert pending.isEmpty() : "" + module + ": I expect the set of pending constraints to be empty when a module is marked as clean, otherwise why did we add the constraints in the first place?";
        
        //the incompleteness should only be filled with these constraints, so we don't actually need to resolve the completeness if it is empty?
        state.solver().getCompleteness().removeAll(pending, state.unifier());
        
        //We do have to remove the initialization to make it complete
        state.solver().getCompleteness().remove(module.getInitialization(), state.unifier());
    }
}
