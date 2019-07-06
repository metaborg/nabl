package mb.statix.taico.incremental;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import mb.nabl2.terms.ITerm;
import mb.statix.constraints.CResolveQuery;
import mb.statix.scopegraph.terms.Scope;
import mb.statix.solver.IConstraint;
import mb.statix.taico.incremental.changeset.IChangeSet;
import mb.statix.taico.incremental.manager.QueryIncrementalManager;
import mb.statix.taico.module.IModule;
import mb.statix.taico.scopegraph.reference.TrackingNameResolution;
import mb.statix.taico.solver.SolverContext;
import mb.statix.taico.solver.query.QueryDetails;
import mb.statix.taico.solver.state.IMState;

public class QueryAnalyzer {
    private IChangeSet changeSet;
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
        
        
        
    }
    
    public void checkQueries(IModule module) {
        //TODO We need to know based on the
        
        Flag flag = module.getTopFlag();
        if (flag.getCleanliness().isCleanish()) {
            //This module is clean.
            switchToClean(module);
        }
        
        IMState state = module.getCurrentState();
        
        //TODO This code needs to run after the solver has been created, but before the runner can become "stuck".
        //TODO IMPORTANT In other words, not here.
        for (Entry<CResolveQuery, QueryDetails> e : module.queries().entrySet()) {
            CResolveQuery query = e.getKey();
            state.solver().getStore().add(query);
        }
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
