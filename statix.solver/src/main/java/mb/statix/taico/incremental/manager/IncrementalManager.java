package mb.statix.taico.incremental.manager;

import java.io.Serializable;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import mb.statix.solver.IConstraint;
import mb.statix.taico.incremental.Flag;
import mb.statix.taico.module.IModule;
import mb.statix.taico.solver.MSolverResult;
import mb.statix.taico.solver.ModuleSolver;
import mb.statix.taico.solver.SolverContext;

public class IncrementalManager implements Serializable {
    private static final long serialVersionUID = 1L;
    
    protected volatile Object phase;
    protected boolean initPhase = true;
    
    // --------------------------------------------------------------------------------------------
    // Phase related
    // --------------------------------------------------------------------------------------------
    
    @SuppressWarnings("unchecked")
    public <T> T getPhase() {
        return (T) phase;
    }
    
    public void setPhase(Object phase) {
        this.phase = phase;
    }
    
    /**
     * @return
     *      if the init phase is currently in progress
     * 
     * @see #finishInitPhase()
     */
    public boolean isInitPhase() {
        return initPhase;
    }
    
    /**
     * Called to signal the end of the initialization phase.
     * During the initialization phase, module access is unrestricted to allow creating the
     * necessary components used for solving. The actual solving process begins with
     * {@link #startFirstPhase()}.
     */
    public void finishInitPhase() {
        initPhase = true;
    }
    
    /**
     * Starts the first phase by creating solvers and adding them to the coordinator.
     * 
     * @param modules
     *      the initial (file) modules
     */
    public void startFirstPhase(Map<IModule, IConstraint> modules) {
        for (Entry<IModule, IConstraint> entry : modules.entrySet()) {
            //childSolver sets the solver on the state and adds it
            ModuleSolver parentSolver = SolverContext.context().getState(entry.getKey().getParentId()).solver();
            parentSolver.childSolver(entry.getKey().getCurrentState(), entry.getValue());
        }
    }
    
    /**
     * Called after a solving phase has completed.
     * 
     * If this method returns true, a new phase is started immediately. Solvers will be readded to
     * the coordinator for the new phase, so the coordinator is free to throw away all existing
     * solvers before calling this method.
     * 
     * @param finishedSolvers
     *      the solvers that finished in the round
     * @param failedSolvers
     *      the solvers that failed in the round
     * @param stuckSolvers
     *      the solvers that became stuck in the round
     * @param results
     *      the results of the round
     * 
     * @return
     *      true if another phase should be started, false if solving is done
     */
    public boolean finishPhase(Set<ModuleSolver> finishedSolvers, Set<ModuleSolver> failedSolvers,
            Set<ModuleSolver> stuckSolvers, Map<IModule, MSolverResult> results) {
        return false;
    }
    
    // --------------------------------------------------------------------------------------------
    // Solver hooks
    // --------------------------------------------------------------------------------------------
    
    /**
     * Called whenever a module solver is initialized.
     * 
     * @param solver
     *      the solver
     */
    public void initSolver(ModuleSolver solver) {
        
    }
    
    /**
     * Called whenever the solver is about to start.
     * 
     * @param solver
     *      the solver
     */
    public void solverStart(ModuleSolver solver) {
        
    }
    
    /**
     * Called whenever the solver is done. This updates the flags of the owner of the solver.
     * 
     * @param solver
     *      the solver
     */
    public void solverDone(ModuleSolver solver) {
        solver.getOwner().setFlag(Flag.CLEAN);
    }
    
    // --------------------------------------------------------------------------------------------
    // Other
    // --------------------------------------------------------------------------------------------

    @Override
    public String toString() {
        return "IncrementalManager [phase=" + phase + ", initPhase=" + initPhase + "]";
    }
}
