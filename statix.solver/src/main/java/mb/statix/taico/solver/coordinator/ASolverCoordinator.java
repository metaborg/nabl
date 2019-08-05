package mb.statix.taico.solver.coordinator;

import static mb.statix.taico.util.TDebug.*;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.Consumer;

import org.metaborg.util.log.Level;
import org.metaborg.util.log.LoggerUtils;

import mb.nabl2.terms.ITermVar;
import mb.statix.solver.Delay;
import mb.statix.solver.IConstraint;
import mb.statix.solver.ISolverResult;
import mb.statix.solver.log.IDebugContext;
import mb.statix.solver.log.LoggerDebugContext;
import mb.statix.taico.incremental.changeset.IChangeSet;
import mb.statix.taico.incremental.strategy.IncrementalStrategy;
import mb.statix.taico.module.IModule;
import mb.statix.taico.solver.Context;
import mb.statix.taico.solver.MSolverResult;
import mb.statix.taico.solver.ModuleSolver;
import mb.statix.taico.solver.progress.ProgressTrackerRunnable;
import mb.statix.taico.solver.state.IMState;

/**
 * Abstract class with the basis of a solver coordinator. A solver coordinator coordinates the
 * solving process when multiple modules are used. The coordinator can chooose to solve modules
 * sequentially or in parallel.
 */
public abstract class ASolverCoordinator implements ISolverCoordinator {
    protected ModuleSolver root;
    protected IMState rootState;
    
    protected IncrementalStrategy strategy;
    protected IDebugContext debug;
    protected Context context;
    protected ProgressTrackerRunnable progressPrinter;
    
    @Override
    public ModuleSolver getRootSolver() {
        return root;
    }
    
    @Override
    public IMState getRootState() {
        return rootState;
    }
    
    @Override
    public IModule getRootModule() {
        return rootState.getOwner();
    }
    
    @Override
    public void setContext(Context context) {
        this.context = context;
    }
    
    @Override
    public Context getContext() {
        return context;
    }
    
    @Override
    public abstract Map<IModule, MSolverResult> getResults();
    
    @Override
    public abstract Collection<ModuleSolver> getSolvers();
    
    @Override
    public abstract void addSolver(ModuleSolver solver);
    
    /**
     * Initializes the solver coordinator with the given root state and constraints.
     * 
     * @param strategy
     *      the incremental strategy
     * @param rootState
     *      the root state
     * @param constraint
     *      the constraint of the root module
     * @param debug
     *      the debug context
     */
    protected void init(IncrementalStrategy strategy, IMState rootState, IConstraint constraint, IDebugContext debug) {
        this.debug = createDebug(COORDINATOR_LEVEL);
        this.rootState = rootState;
        this.root = ModuleSolver.topLevelSolver(rootState, constraint, debug);
        this.strategy = strategy;
        if (PROGRESS_TRACKER_INTERVAL > 0) {
            this.progressPrinter = new ProgressTrackerRunnable(PROGRESS_TRACKER_INTERVAL);
            this.progressPrinter.start();
        }
    }
    
    /**
     * Deinitializes the coordinator by stopping the progress tracker.
     */
    protected void deinit() {
        if (this.progressPrinter != null) {
            this.progressPrinter.stop();
        }
    }
    
    // --------------------------------------------------------------------------------------------
    // Solving
    // --------------------------------------------------------------------------------------------
    
    @Override
    public abstract MSolverResult solve(IMState state, IConstraint constraint, IDebugContext debug) throws InterruptedException;
    
    @Override
    public abstract void solveAsync(IMState state, IConstraint constraints, IDebugContext debug, Consumer<MSolverResult> onFinished);
    
    @Override
    public Map<String, ISolverResult> solve(IncrementalStrategy strategy, IChangeSet changeSet, IMState state, Map<String, IConstraint> constraints, IDebugContext debug)
            throws InterruptedException {
        init(strategy, state, null, debug);
        
        preventSolverStart();
        Map<IModule, IConstraint> modules = strategy.createInitialModules(context, changeSet, constraints);
        
        if (context.isInitPhase()) context.finishInitPhase();
        scheduleModules(modules);
        allowSolverStart();
        
        runToCompletion();
        
        return collectResults(modules.keySet());
    }
    
    /**
     * Makes sure this coordinator progresses until completion. This method will call
     * finishRound to determine if more rounds are requested.
     * <p>
     * Whenever this method returns, {@link #finishSolving()} will have been called.
     * 
     * @throws InterruptedException
     *      If the coordinator is interrupted while running.
     */
    protected abstract void runToCompletion() throws InterruptedException;
    
    /**
     * Schedules the given modules by creating solvers for them. Also schedules the root solver.
     * 
     * @param modules
     *      the modules to schedule
     */
    protected void scheduleModules(Map<IModule, IConstraint> modules) {
        addSolver(root);
        context.getIncrementalManager().startFirstPhase(modules);
    }
    
    // --------------------------------------------------------------------------------------------
    // Phasing
    // --------------------------------------------------------------------------------------------
    
    /**
     * Finishes the current phase. If another phase is started, this method will return true.
     * Otherwise, this method returns false.
     * 
     * @return
     *      true if we want another phase of solving, false otherwise
     */
    protected boolean finishPhase() {
        Map<IModule, MSolverResult> results = getResults();
        Collection<ModuleSolver> solvers = getSolvers();
        
        Set<ModuleSolver> finishedSolvers = new HashSet<>();
        Set<ModuleSolver> failedSolvers = new HashSet<>();
        //All the solvers that are still remaining are stuck
        Set<ModuleSolver> stuckSolvers = new HashSet<>(solvers);
        
        for (Entry<IModule, MSolverResult> result : results.entrySet()) {
            if (result.getValue().hasErrors()) {
                failedSolvers.add(result.getKey().getCurrentState().solver());
            } else if (result.getValue().hasDelays()) {
                stuckSolvers.add(result.getKey().getCurrentState().solver());
            } else {
                finishedSolvers.add(result.getKey().getCurrentState().solver());
            }
        }
        
        int failed = (int) results.values().stream().filter(r -> r.hasErrors()).count();
        debug.info("Phase {} finished: {} done, {} failed, {} stuck",
                context.getPhase() == null ? "init" : context.getPhase().toString(),
                results.size() - failed,
                failed,
                solvers.size());
        
        if (failed > 0) {
            debug.info("Failed solvers:");
            IDebugContext sub = debug.subContext();
            results.entrySet().stream().filter(e -> e.getValue().hasErrors()).forEach(e -> sub.info(e.getKey().getId()));
        }
        
        if (!solvers.isEmpty()) {
            debug.info("Stuck solvers:");
            IDebugContext sub = debug.subContext();
            for (ModuleSolver solver : solvers) {
                sub.info(solver.getOwner().getId());
                
                //NOTE: This adds results to the results map
                results.put(solver.getOwner(), solver.finishSolver());
            }
            
            solvers.clear();
        }
        
        if (!startNextPhase(finishedSolvers, failedSolvers, stuckSolvers, results)) {
            results.clear();
            results.putAll(context.getIncrementalManager().getResults());
            return false;
        }
        
        debug.info("Starting new phase: {}", context.<Object>getPhase());
        
        //Clear all results before starting the next phase
        results.clear();
        return true;
    }
    
    /**
     * Called after the last phase has finished.
     * <p>
     * Finishes the solving process by outputting debug info and more.
     */
    protected void finishSolving() {
        deinit();
        logDebugInfo(debug);
    }
    
    /**
     * Called whenever an unexpected exception is encountered while solving.
     * This method will also call {@link #finishSolving()} to finish the solving process.
     */
    protected void failSolving(Exception ex) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        ex.printStackTrace(pw);
        System.err.println("Solving failed:");
        ex.printStackTrace();
        
        debug.error("Solving failed with an exception: {}", pw.toString());
        finishSolving();
        debug.error("Solving failed with an exception! (see above)");
    }
    
    /**
     * Checks if we want to execute another phase. If we do, this method starts the next phase and
     * returns true. Otherwise, this method returns false.
     * 
     * @param finishedSolvers
     *      finished solvers
     * @param failedSolvers
     *      failed solvers
     * @param stuckSolvers
     *      stuck solvers
     * @param results
     *      results of the phase
     * 
     * @return
     *      true if another phase has started, false otherwise
     */
    protected boolean startNextPhase(
            Set<ModuleSolver> finishedSolvers, Set<ModuleSolver> failedSolvers,
            Set<ModuleSolver> stuckSolvers, Map<IModule, MSolverResult> results) {
        return context.getIncrementalManager().finishPhase(finishedSolvers, failedSolvers, stuckSolvers, results);
    }
    
    // --------------------------------------------------------------------------------------------
    // Gather results
    // --------------------------------------------------------------------------------------------
    
    /**
     * Creates a map with all the results from module <b>NAME</b> to solver result.
     * The returned map will only contain results for modules present in the given collection. 
     * 
     * @param modules
     *      the modules to return results for
     * 
     * @return
     *      the results of the given modules
     */
    protected Map<String, ISolverResult> collectResults(Collection<IModule> modules) {
        Map<String, ISolverResult> results = new HashMap<>();
        for (Entry<IModule, MSolverResult> entry : getResults().entrySet()) {
            //Skip all modules that are not in the initial set.
            if (!modules.contains(entry.getKey())) continue;
            
            IModule fileModule = entry.getKey();
            Set<IConstraint> errors = new LinkedHashSet<>();
            Map<IConstraint, Delay> delays = new LinkedHashMap<>();
            ISolverResult fileResult = entry.getValue();
            errors.addAll(fileResult.errors());
            delays.putAll(fileResult.delays());
            
            for (IModule module : (Iterable<IModule>) fileModule.getDescendants()::iterator) {
                ISolverResult result = getResults().get(module);
                errors.addAll(result.errors());
                delays.putAll(result.delays());
            }
            results.put(entry.getKey().getName(), fileResult.withErrors(errors).withDelays(delays));
        }
        return results;
    }
    
    /**
     * Aggregates results of all the solvers into one SolverResult.
     * 
     * @return
     *      the aggregated results
     */
    protected MSolverResult aggregateResults() {
        //If there is only one result, we have nothing to aggregate
        if (getResults().size() == 1) {
            return getResults().values().stream().findFirst().get();
        }
        
        //TODO Instead of aggregating results, we should perhaps return the results of the top module?
        Set<IConstraint> errors = new LinkedHashSet<>();
        Map<IConstraint, Delay> delays = new LinkedHashMap<>();
        Map<ITermVar, ITermVar> existentials = getResults().get(root.getOwner()).existentials();
        for (Entry<IModule, MSolverResult> result : getResults().entrySet()) {
            errors.addAll(result.getValue().errors());
            delays.putAll(result.getValue().delays());
        }
        
        //TODO Use a fake top state instead of the root state? Because errors might need to be moved to other locations.
        return MSolverResult.of(getRootState(), errors, delays, existentials);
    }
    
    // --------------------------------------------------------------------------------------------
    // Other
    // --------------------------------------------------------------------------------------------
    
    /**
     * Creates a debug context for the coordinator.
     * 
     * @param level
     *      the level
     * 
     * @return
     *      the debug context
     */
    private static IDebugContext createDebug(Level level) {
        return new LoggerDebugContext(LoggerUtils.logger("Coordinator"), level);
    }
}
