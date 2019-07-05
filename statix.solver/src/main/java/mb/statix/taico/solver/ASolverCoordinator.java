package mb.statix.taico.solver;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.Consumer;

import mb.nabl2.terms.ITermVar;
import mb.statix.solver.Delay;
import mb.statix.solver.IConstraint;
import mb.statix.solver.ISolverResult;
import mb.statix.solver.log.IDebugContext;
import mb.statix.solver.log.LazyDebugContext;
import mb.statix.solver.log.PrefixedDebugContext;
import mb.statix.taico.incremental.changeset.IChangeSet2;
import mb.statix.taico.incremental.strategy.IncrementalStrategy;
import mb.statix.taico.module.IModule;
import mb.statix.taico.solver.progress.ProgressTrackerRunnable;
import mb.statix.taico.util.TDebug;

public abstract class ASolverCoordinator {
    protected ModuleSolver root;
    protected IMState rootState;
    
    protected IncrementalStrategy strategy;
    protected IDebugContext debug;
    protected SolverContext context;
    protected ProgressTrackerRunnable progressPrinter;
    
    /**
     * @return
     *      the root solver
     */
    public ModuleSolver getRootSolver() {
        return root;
    }
    
    /**
     * @return
     *      the state of the root solver
     */
    public IMState getRootState() {
        return rootState;
    }
    
    /**
     * @return
     *      the root module
     */
    public IModule getRootModule() {
        return rootState.getOwner();
    }
    
    /**
     * Sets the context that is reported to the SolverRunnables.
     * 
     * @param context
     *      the context
     */
    public void setContext(SolverContext context) {
        this.context = context;
    }
    
    /**
     * @return
     *      the context that this coordinator is currently coordinating
     */
    public SolverContext getContext() {
        return context;
    }
    
    /**
     * @return
     *      a map containing all the solver results of solvers that have completed
     */
    public abstract Map<IModule, MSolverResult> getResults();
    
    /**
     * @return
     *      a collection of all solvers
     */
    public abstract Collection<ModuleSolver> getSolvers();
    
    /**
     * Adds the given solver to the collection of solvers.
     * 
     * @param solver
     *      the solver to add
     */
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
        this.debug = new PrefixedDebugContext("Coordinator", debug);
        this.rootState = rootState;
        this.root = ModuleSolver.topLevelSolver(rootState, constraint, debug);
        this.strategy = strategy;
        if (TDebug.PROGRESS_TRACKER_INTERVAL > 0) {
            this.progressPrinter = new ProgressTrackerRunnable(TDebug.PROGRESS_TRACKER_INTERVAL);
            this.progressPrinter.start();
        }
    }
    
    protected void deinit() {
        if (this.progressPrinter != null) {
            this.progressPrinter.stop();
        }
    }
    
    /**
     * Solves the given constraints in a modularized fashion.
     * 
     * @param state
     *      the state of the root module
     * @param constraint
     *      the constraint to solve
     * @param debug
     *      the debug context to log to
     * 
     * @return
     *      the aggregated result of solving
     * 
     * @throws InterruptedException
     *      If solving is interrupted.
     */
    public abstract MSolverResult solve(IMState state, IConstraint constraint, IDebugContext debug) throws InterruptedException;
    
    /**
     * Solves the given constraints in a modularized fashion.
     * 
     * This method starts the solving process and then returns a future which can be used to 
     * 
     * @param state
     *      the state of the root module
     * @param constraints
     *      the constraints to solve
     * @param debug
     *      the debug context to log to
     * @param onFinished
     *      called whenever solving is finished
     * 
     * @return
     *      a future to get the solve result from
     * 
     * @throws UnsupportedOperationException
     *      If this solver does not support asynchronous solving.
     */
    public abstract void solveAsync(IMState state, IConstraint constraints, IDebugContext debug, Consumer<MSolverResult> onFinished);
    
    
    public Map<String, ISolverResult> solve(IncrementalStrategy strategy, IChangeSet2 changeSet, IMState state, Map<String, IConstraint> constraints, IDebugContext debug)
            throws InterruptedException {
        init(strategy, state, null, debug);
        
        Map<IModule, IConstraint> modules = strategy.createModulesForPhase(context, changeSet, constraints);
        
        if (context.isInitPhase()) context.finishInitPhase();
        scheduleModules(modules);
        
        try {
            runToCompletion();
        } finally {
            deinit();
        }
        
        return collectResults(modules.keySet());
    }
    
    /**
     * Schedules the given modules by creating solvers for them.
     * 
     * @param modules
     *      the modules to schedule
     */
    protected void scheduleModules(Map<IModule, IConstraint> modules) {
        for (Entry<IModule, IConstraint> entry : modules.entrySet()) {
            //childSolver sets the solver on the state and adds it
            root.childSolver(entry.getKey().getCurrentState(), entry.getValue());
        }
    }
    
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
            results.put(entry.getKey().getName(), entry.getValue());
        }
        return results;
    }
    
    /**
     * Makes sure this coordinator progresses until completion, and then returns.
     * 
     * @throws InterruptedException
     *      If the coordinator is interrupted while running.
     */
    protected abstract void runToCompletion() throws InterruptedException;
    
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
        return MSolverResult.of(getRootState(), errors, delays, existentials);
    }
    
    /**
     * Logs debug output.
     * 
     * @param debug
     *      the debug context to log to
     */
    public void logDebugInfo(IDebugContext debug) {
        debug.info("Debug output.");
        debug.info("Module hierarchy:");
        printModuleHierarchy(getRootState().owner(), debug.subContext());
        
        LazyDebugContext success = new LazyDebugContext(debug.subContext());
        LazyDebugContext fail = new LazyDebugContext(debug.subContext());
        LazyDebugContext stuck = new LazyDebugContext(debug.subContext());
        LazyDebugContext failDetails = new LazyDebugContext(debug.subContext().subContext());
        LazyDebugContext stuckDetails = new LazyDebugContext(debug.subContext().subContext());
        
        for (Entry<IModule, MSolverResult> entry : getResults().entrySet()) {
            String id = entry.getKey().getId();
            if (entry.getValue().hasErrors()) {
                fail.info(id);
                failDetails.info("[{}] Failed constraints:", id);
                IDebugContext sub = failDetails.subContext();
                for (IConstraint c : entry.getValue().errors()) {
                    sub.info(c.toString());
                }
            } else if (entry.getValue().hasDelays()) {
                stuck.info(id);
                stuckDetails.info("[{}] Stuck constraints:", id);
                IDebugContext sub = stuckDetails.subContext();
                for (Entry<IConstraint, Delay> e : entry.getValue().delays().entrySet()) {
                    Delay delay = e.getValue();
                    if (!delay.vars().isEmpty()) {
                        sub.info("on vars {}: {}", delay.vars(), e.getKey());
                    } else if (!delay.criticalEdges().isEmpty()) {
                        sub.info("on edges {}: {}", delay.criticalEdges(), e.getKey());
                    } else {
                        sub.info("on unknown: {}", e.getKey());
                    }
                }
            } else {
               success.info(id); 
            }
        }
        
        debug.info("Finished modules:");
        success.commit();
        
        debug.info("Stuck modules:");
        stuck.commit();
        
        debug.info("Failed modules:");
        fail.commit();
        
        debug.info("Stuck output:");
        stuckDetails.commit();
        
        debug.info("Failed output:");
        failDetails.commit();
    }
    
    /**
     * Prints the module hierarchy to the given context, starting at the given module.
     * 
     * @param module
     *      the module to start at
     * @param context
     *      the debug context to print to
     */
    public void printModuleHierarchy(IModule module, IDebugContext context) {
        context.info("{}: dependencies={}", module.getId(), module.getDependencies());
        IDebugContext sub = context.subContext();
        for (IModule child : module.getChildren()) {
            printModuleHierarchy(child, sub);
        }
    }
}
