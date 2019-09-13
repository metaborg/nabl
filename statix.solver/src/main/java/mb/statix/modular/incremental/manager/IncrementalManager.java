package mb.statix.modular.incremental.manager;

import static mb.statix.modular.util.TOverrides.SPLIT_MODULES;

import java.io.Serializable;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.Callable;

import mb.statix.modular.incremental.Flag;
import mb.statix.modular.module.IModule;
import mb.statix.modular.module.split.SplitModuleUtil;
import mb.statix.modular.solver.Context;
import mb.statix.modular.solver.MSolverResult;
import mb.statix.modular.solver.ModuleSolver;
import mb.statix.modular.util.TOverrides;
import mb.statix.solver.IConstraint;

public class IncrementalManager implements Serializable {
    private static final long serialVersionUID = 1L;
    
    protected volatile Object phase;
    protected boolean initPhase = true;
    protected Set<String> nonSplitModules = TOverrides.set();
    protected Set<String> allowedTemporarily = TOverrides.set();
    protected Map<IModule, MSolverResult> results = TOverrides.hashMap();
    
    // --------------------------------------------------------------------------------------------
    // Getters
    // --------------------------------------------------------------------------------------------
    
    public Map<IModule, MSolverResult> getResults() {
        return results;
    }
    
    // --------------------------------------------------------------------------------------------
    // Module access
    // --------------------------------------------------------------------------------------------

    /**
     * Registers that a module is not yet split.
     * 
     * @param id
     *      the non-split module
     */
    public void registerNonSplit(String id) {
        if (!SPLIT_MODULES) return;
        
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
        if (!SPLIT_MODULES) return true;
        
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
        allowedTemporarily.add(id);
        try {
            runnable.run();
        } finally {
            allowedTemporarily.remove(id);
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
        allowedTemporarily.add(id);
        try {
            tbr = callable.call();
        } finally {
            allowedTemporarily.remove(id);
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
        return SPLIT_MODULES;
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
        if (isInitPhase() || isAllowedTemporarily(requester)) return true;
        if (requester.equals(moduleId)) return true;
        return !SPLIT_MODULES || !nonSplitModules.contains(requester);
    }
    
    /**
     * @param requester
     *      the id of the requesting module
     * 
     * @return
     *      true if this module is temporarily allowed to access other modules, false otherwise
     */
    public boolean isAllowedTemporarily(String requester) {
        return allowedTemporarily.contains(requester);
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
        initPhase = false;
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
            ModuleSolver parentSolver = Context.context().getState(entry.getKey().getParentId()).solver();
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
     * Called whenever a solver is initialized.
     * 
     * @param solver
     *      the solver
     */
    public void initSolver(ModuleSolver solver) {
        
    }
    
    /**
     * Called whenever a solver (including separate solvers) is about to start.
     * 
     * @param solver
     *      the solver
     */
    public void solverStart(ModuleSolver solver) {
        
    }
    
    /**
     * Called whenever a solver is done. This updates the flags of the owner of the solver.
     * 
     * @param solver
     *      the solver
     */
    public void solverDone(ModuleSolver solver, MSolverResult result) {
        if (solver.isSeparateSolver()) return;
        solver.getOwner().setFlag(Flag.CLEAN);
        results.put(solver.getOwner(), result);
    }
    
    // --------------------------------------------------------------------------------------------
    // Other
    // --------------------------------------------------------------------------------------------

    /**
     * Determines if the given child may be transferred to the current context instead of creating
     * it anew with the given constraint.
     * 
     * @param oldModule
     *      the old module
     * @param constraint
     *      the new creation constraint
     * 
     * @return
     *      true if the child may be transferred, false otherwise
     */
    public boolean allowTransferChild(IModule oldModule, IConstraint constraint) {
        return false;
    }
    
    @Override
    public String toString() {
        return "IncrementalManager [phase=" + phase + ", initPhase=" + initPhase + "]";
    }

    public void wipe() {
        if (nonSplitModules != null) nonSplitModules.clear();
        nonSplitModules = null;
        if (results != null) results.clear();
        results = null;
        if (allowedTemporarily != null) allowedTemporarily.clear();
    }
    
    public int getPhaseCount() {
        return (Integer) phase;
    }
    public int getReanalyzedModuleCount() {
        return 0;
    }
    public long getTotalDiffTime() {
        return 0;
    }
    public long getTotalDependencyTime() {
        return 0;
    }
}
