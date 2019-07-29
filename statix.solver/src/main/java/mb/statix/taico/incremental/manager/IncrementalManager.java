package mb.statix.taico.incremental.manager;

import java.io.Serializable;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.Callable;

import mb.statix.solver.IConstraint;
import mb.statix.taico.incremental.Flag;
import mb.statix.taico.module.IModule;
import mb.statix.taico.module.split.SplitModuleUtil;
import mb.statix.taico.solver.MSolverResult;
import mb.statix.taico.solver.ModuleSolver;
import mb.statix.taico.solver.SolverContext;
import mb.statix.taico.util.TOverrides;

public class IncrementalManager implements Serializable {
    private static final long serialVersionUID = 1L;
    
    protected volatile Object phase;
    protected boolean initPhase = true;
    protected Set<String> nonSplitModules = TOverrides.set();
    
    // Module access

    /**
     * Registers that a module is not yet split.
     * 
     * @param id
     *      the non-split module
     */
    public void registerNonSplit(String id) {
        System.err.println("Registering " + id + " as non split (made restricted)");
        assert !SplitModuleUtil.isSplitModule(id) : "Registration of a non split module expects a non-split module!";
        nonSplitModules.add(id);
    }
    
    /**
     * Removes the registration for a non-split module (because the module has been split).
     * 
     * @param id
     *      the module
     * 
     * @return
     *      if the module was unregistered
     */
    public boolean unregisterNonSplit(String id) {
        if (nonSplitModules.remove(id)) {
            System.err.println("Unregistering " + id + " as non split (made unrestricted)");
            return true;
        }
        
        return false;
    }
    
    /**
     * Executes the given runnable where the module with the given id is unrestricted.
     * 
     * @param id
     *      the id of the module
     * @param runnable
     *      the runnable to execute
     */
    public void executeUnrestricted(String id, Runnable runnable) {
        boolean removed = nonSplitModules.remove(id);
        try {
            runnable.run();
        } finally {
            if (removed) nonSplitModules.add(id);
        }
    }
    
    /**
     * Executes the given Callable where the module with the given id is unrestricted.
     * 
     * @param id
     *      the id of the module
     * @param callable
     *      the callable to execute
     * 
     * @return
     *      the result of the callable
     * 
     * @throws Exception
     *      If the callable throws an exception when called.
     */
    public <R> R executeUnrestricted(String id, Callable<R> callable) throws Exception {
        R tbr;
        boolean removed = nonSplitModules.remove(id);
        try {
            tbr = callable.call();
        } finally {
            if (removed) nonSplitModules.add(id);
        }
        return tbr;
    }
    
    /**
     * Called to inform the incremental manager of the fact that in normal execution, a new split
     * module would now be created for the given module. However, the strategy, through its
     * incremental manager is free to change this behavior.
     * 
     * @return
     *      true if a split module should be created, false if not
     */
    public boolean createSplitModuleRequest(String id) {
        return true;
    }
    
    /**
     * @param requester
     *      the id of the requesting module
     * @param moduleId
     *      the id of the module that is requested
     * 
     * @return
     *      true if access to the given module is allowed, false otherwise.
     */
    public boolean isAllowedAccess(String requester, String moduleId) {
        if (requester.equals(moduleId)) return true;
        return !nonSplitModules.contains(moduleId);
    }
    
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
