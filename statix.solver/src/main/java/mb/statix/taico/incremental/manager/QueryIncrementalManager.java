package mb.statix.taico.incremental.manager;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import mb.statix.solver.IConstraint;
import mb.statix.taico.incremental.Flag;
import mb.statix.taico.module.IModule;
import mb.statix.taico.solver.IMState;
import mb.statix.taico.solver.ModuleSolver;
import mb.statix.taico.solver.SolverContext;

public class QueryIncrementalManager extends IncrementalManager {
    private Set<String> allowedAccess = ConcurrentHashMap.newKeySet();
    
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
    
    public boolean isAllowedAccess(String module) {
        return allowedAccess.contains(module);
    }
    
    public void allowAccess(String module) {
        allowedAccess.add(module);
    }
    
    @Override
    public void solverStart(ModuleSolver solver) {
        solver.getStore().enableExternalMode();
    }
    
    @Override
    public void solverDone(ModuleSolver solver) {
        System.err.println("Solver done triggered on incremental manager for " + solver.getOwner() + ". Switching module over to clean.");
        switchToClean(solver.getOwner());
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
    
    //TODO I need a mechanism for a module to become clean, e.g. the queries need to be checked themselves and then the module needs to be marked as clean.
    
    public static enum QueryPhase {
        Dirty,
        
        
        /** The final phase where we just do normal solving. */
        Final
        
    }
}
