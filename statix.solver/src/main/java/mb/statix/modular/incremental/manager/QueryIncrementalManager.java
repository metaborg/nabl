package mb.statix.modular.incremental.manager;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import mb.statix.constraints.CResolveQuery;
import mb.statix.modular.dependencies.details.QueryDependencyDetail;
import mb.statix.modular.incremental.Flag;
import mb.statix.modular.module.IModule;
import mb.statix.modular.solver.Context;
import mb.statix.modular.solver.MSolverResult;
import mb.statix.modular.solver.ModuleSolver;
import mb.statix.modular.solver.state.IMState;
import mb.statix.solver.IConstraint;

public class QueryIncrementalManager extends IncrementalManager {
    private static final long serialVersionUID = 1L;
    
    private Set<String> allowedAccess = ConcurrentHashMap.newKeySet();
    
    // --------------------------------------------------------------------------------------------
    // Phase
    // --------------------------------------------------------------------------------------------
    
    @Override
    public void setPhase(Object phase) {
        if (!(phase instanceof QueryPhase)) throw new IllegalArgumentException("Can only switch the phase to a query phase.");
        
        QueryPhase oldPhase = getPhase();
        super.setPhase(phase);
        updatePhase(oldPhase, (QueryPhase) phase);
    }
    
    protected void updatePhase(QueryPhase oldPhase, QueryPhase newPhase) {
        //TODO IMPORTANT
    }

    @Override
    public boolean finishPhase(Set<ModuleSolver> finishedSolvers, Set<ModuleSolver> failedSolvers,
            Set<ModuleSolver> stuckSolvers, Map<IModule, MSolverResult> results) {
        //TODO Check all the modules that are not done at this point, and do the setup for the next phase (expand the set of constraints for solving, to make more progress)
        throw new UnsupportedOperationException("TODO Implement phase switching. Do the setup for the next phase here.");
    }
    
    // --------------------------------------------------------------------------------------------
    // Access restrictions
    // --------------------------------------------------------------------------------------------
    
    public boolean isAllowedAccess(String module) {
        return allowedAccess.contains(module);
    }
    
    public void allowAccess(String module) {
        allowedAccess.add(module);
    }
    
    // --------------------------------------------------------------------------------------------
    // Phase
    // --------------------------------------------------------------------------------------------
    
    @Override
    public void initSolver(ModuleSolver solver) {
        if (solver.isSeparateSolver()) return;
        
        //On initialization, we want to add the init constraint of the module to the completeness
        //to mark it as imcomplete.
        //TODO This does not work properly (the above)
        
        IModule module = solver.getOwner();
        if (module.getTopCleanliness().isCleanish()) return;
        
        IMState state = solver.getOwner().getCurrentState();
        solver.getCompleteness().add(solver.getOwner().getInitialization(), state.unifier());
    }
    
    @Override
    public void solverStart(ModuleSolver solver) {
        System.err.println("Solver start triggerd for " + solver.getOwner());
//        solver.getStore().enableExternalMode();
        
        //TODO IMPORTANT REFINE THIS
        //Add the queries to the store
        addQueries(solver.getOwner());
    }
    
    @Override
    public void solverDone(ModuleSolver solver, MSolverResult result) {
        if (solver.isSeparateSolver()) return;
        System.err.println("Solver done triggered on incremental manager for " + solver.getOwner() + ". Switching module over to clean.");
        switchToClean(solver.getOwner());
        results.put(solver.getOwner(), result);
    }
    
    public void switchToClean(IModule module) {
        //If the module is already clean, then we don't have to do anything
        if (module.getTopCleanliness().isCleanish()) {
            System.err.println("Module " + module + " is already clean, not switching to clean.");
            return;
        }
        
        //Set as clean
        module.setFlag(Flag.CLEAN);
        
        //Allow module access
        Context.context().<QueryIncrementalManager>getIncrementalManager().allowAccess(module.getId());
        
        //Resolve pending incompleteness
        IMState state = module.getCurrentState();
        Set<IConstraint> pending = state.solver().getStore().getAllRemainingConstraints();
        //The pending set should be empty
        assert pending.isEmpty() : "" + module + ": I expect the set of pending constraints to be empty when a module is marked as clean, otherwise why did we add the constraints in the first place?";
        
        //the incompleteness should only be filled with these constraints, so we don't actually need to resolve the completeness if it is empty?
        state.solver().getCompleteness().removeAll(pending, state.unifier());
        
        //We do have to remove the initialization to make it complete
        state.solver().getCompleteness().remove(module.getInitialization(), state.unifier());
        
        //TODO Fix flags of other modules? Does this happen automatically?
    }
    
    public void addQueries(IModule module) {
        //TODO We need to know based on the
        
        Flag flag = module.getTopFlag();
        if (flag.getCleanliness().isCleanish()) {
            //This module is clean.
            switchToClean(module);
        }
        
        IMState state = module.getCurrentState();
        
        //TODO This code needs to run after the solver has been created, but before the runner can become "stuck".
        //TODO IMPORTANT In other words, not here.
        for (QueryDependencyDetail qdd : module.queries().keySet()) {
            CResolveQuery query = qdd.getOriginalConstraint();
            state.solver().getStore().add(query);
        }
        //The given module should be a clirty one. We now have to redo it's queries
        //TrackingNameResolution<Scope, ITerm, ITerm> nameResolution = TrackingNameResolution.builder();
    }
    
    //TODO I need a mechanism for a module to become clean, e.g. the queries need to be checked themselves and then the module needs to be marked as clean.
    
    public static enum QueryPhase {
        Dirty,
        
        
        /** The final phase where we just do normal solving. */
        Final
        
    }
}
