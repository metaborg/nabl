package mb.statix.taico.solver.concurrent;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

import mb.statix.solver.IConstraint;
import mb.statix.solver.ISolverResult;
import mb.statix.solver.log.IDebugContext;
import mb.statix.taico.incremental.changeset.IChangeSet;
import mb.statix.taico.incremental.strategy.IncrementalStrategy;
import mb.statix.taico.incremental.strategy.NonIncrementalStrategy;
import mb.statix.taico.module.IModule;
import mb.statix.taico.solver.MSolverResult;
import mb.statix.taico.solver.ModuleSolver;
import mb.statix.taico.solver.coordinator.ASolverCoordinator;
import mb.statix.taico.solver.coordinator.ISolverCoordinator;
import mb.statix.taico.solver.state.IMState;

public class ConcurrentSolverCoordinator extends ASolverCoordinator {
    private final Map<ModuleSolver, SolverRunnable> solvers = Collections.synchronizedMap(new HashMap<>());
    private final Map<ModuleSolver, SolverRunnable> failedSolvers = Collections.synchronizedMap(new HashMap<>());
    private final Map<IModule, MSolverResult> results = Collections.synchronizedMap(new HashMap<>());
    private final ProgressCounter progressCounter = new ProgressCounter(this::onFinished);
    protected final ExecutorService executors;
    private StuckDetector stuckDetector;
    
    private Consumer<MSolverResult> onFinished;
    private MSolverResult finalResult;
    
    /**
     * Creates a new concurrent solver coordinator with a work stealing pool. This pool uses the
     * available number of processors as the default parallelism level.
     */
    public ConcurrentSolverCoordinator() {
        this(Executors.newWorkStealingPool());
    }
    
    /**
     * Creates a new concurrent solver coordinator with the given executor service.
     * 
     * @param executors
     *      the executor service
     */
    public ConcurrentSolverCoordinator(ExecutorService executors) {
        this.executors = executors;
    }

    @Override
    public Map<IModule, MSolverResult> getResults() {
        return results;
    }
    
    /**
     * NOTE: Synchronize over the collection if iterating over it while the solving process is
     * still ongoing to avoid ConcurrentModificationException s.
     * 
     * @see ISolverCoordinator#getSolvers()
     */
    @Override
    public Set<ModuleSolver> getSolvers() {
        return solvers.keySet();
    }
    
    @Override
    protected void init(IncrementalStrategy strategy, IMState rootState, IConstraint constraint, IDebugContext debug) {
        this.finalResult = null;
        super.init(strategy, rootState, constraint, debug);
        if (progressPrinter != null) {
            stuckDetector = new StuckDetector(this, progressPrinter.tracker);
            stuckDetector.start();
        }
    }
    
    @Override
    public void addSolver(ModuleSolver solver) {
        SolverRunnable runner = new SolverRunnable(solver, executors::submit, progressCounter, this::finishSuccessfulSolver, this::finishFailedSolver, this::getContext);
        solver.getStore().setStoreObserver(store -> runner.notifyOfWork());
        solvers.put(solver, runner);
        runner.schedule();
    }
    
    private void finishSuccessfulSolver(ModuleSolver solver) {
        synchronized (solvers) {
            SolverRunnable runnable = solvers.remove(solver);
            if (runnable == null) {
                debug.warn("[{}] FinishSolver: ignoring, solver already removed", solver.getOwner().getId());
                return;
            }
            
            failedSolvers.put(solver, runnable);
        }
        
        results.put(solver.getOwner(), solver.finishSolver());
    }
    
    private void finishFailedSolver(ModuleSolver solver) {
        synchronized (solvers) {
            SolverRunnable runnable = solvers.remove(solver);
            if (runnable == null) {
                debug.warn("[{}] FinishSolver: ignoring, solver already removed", solver.getOwner());
                return;
            }
        }
        
        results.put(solver.getOwner(), solver.finishSolver());
    }
    
    @Override
    public MSolverResult solve(IMState state, IConstraint constraint, IDebugContext debug) throws InterruptedException {
        solveAsync(state, constraint, debug, null);
        awaitCompletion();
        return finalResult;
    }
    
    @Override
    public void solveAsync(IMState state, IConstraint constraint, IDebugContext debug, Consumer<MSolverResult> onFinished) {
        this.onFinished = onFinished;
        init(new NonIncrementalStrategy(), state, constraint, debug);
        addSolver(root);
    }
    
    @Override
    public Map<String, ISolverResult> solve(IncrementalStrategy strategy, IChangeSet changeSet, IMState state,
            Map<String, IConstraint> constraints, IDebugContext debug) throws InterruptedException {
        init(strategy, state, null, debug);
        
        Map<IModule, IConstraint> modules = strategy.createModulesForPhase(context, changeSet, constraints);
        
        if (context.isInitPhase()) context.finishInitPhase();
        
        scheduleModules(modules);
        addSolver(root);
        
        try {
            runToCompletion();
        } finally {
            deinit();
        }
        
        return collectResults(modules.keySet());
    }
    
    @Override
    protected void scheduleModules(Map<IModule, IConstraint> modules) {
        //Increase the progress counter to ensure modules do not finish before we are done scheduling them.
        progressCounter.switchToPending();
        try {
            super.scheduleModules(modules);
        } finally {
            progressCounter.switchToWaiting();
        }
    }
    
    private void onFinished() {
        finishSolving(debug);
    }
    
    /**
     * Performs the last part of solving, e.g. collecting results and logging debug information.
     * 
     * @param debug
     *      the debug context to log to
     * 
     * @return
     *      the solver result
     */
    private void finishSolving(IDebugContext debug) {
        //We might want to do another round, prevent triggering this code multiple times.
        progressCounter.switchToPending();

        //Check for another round
        if (context.getIncrementalManager().finishPhase()) {
            //We want to do another round
            if (solvers.isEmpty()) {
                debug.info("Phase done: all solvers finished successfully!");
            } else {
                debug.info("Phase done: solving not completed successfully, {} unsuccessful solvers: ", solvers.size());
            }
            
            //Recycle failed solvers and runnables
            synchronized (failedSolvers) {
                for (Entry<ModuleSolver, SolverRunnable> entry : failedSolvers.entrySet()) {
                    solvers.put(entry.getKey(), entry.getValue());
                    
                    entry.getValue().restart();
                }
                
                failedSolvers.clear();
            }
            
            progressCounter.switchToDone();
            return;
        }
        deinit();
        
        //If we end up here, none of the solvers is still able to make progress
        if (solvers.isEmpty()) {
            //All solvers are done!
            debug.info("All solvers finished successfully!");
        } else {
            debug.warn("Solving failed, {} unsuccessful solvers: ", solvers.size());
            IDebugContext sub = debug.subContext();
            for (ModuleSolver solver : getSolvers()) {
                sub.warn(solver.getOwner().getId());
                
                results.put(solver.getOwner(), solver.finishSolver());
            }
            
            solvers.clear();
        }
        
        logDebugInfo(debug);
        
        finalResult = aggregateResults();
        if (onFinished != null) {
            try {
                onFinished.accept(finalResult);
            } catch (Throwable t) {
                debug.error("On finished consumer threw an exception: {}", t.getMessage());
            }
        }
        synchronized (this) {
            notify();
        }
        
        return;
    }

    @Override
    protected void runToCompletion() throws InterruptedException {
        awaitCompletion();
    }
    
    @Override
    protected void deinit() {
        super.deinit();
        if (this.stuckDetector != null) {
            this.stuckDetector.stop();
        }
        
        //TODO Remove debug info
        System.err.println("Shutting down executor service...");
        System.err.println("Remaining tasks: " + executors.shutdownNow().size());
        System.err.println("Executor service shut down");
    }
    
    /**
     * @return
     *      the progress counter
     */
    public ProgressCounter getProgressCounter() {
        return progressCounter;
    }
    
    /**
     * Awaits the completion of this coordinator.
     * 
     * @throws InterruptedException
     *      
     */
    private synchronized void awaitCompletion() throws InterruptedException {
        while (finalResult == null) {
            wait();
        }
    }
}
