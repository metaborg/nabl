package mb.statix.taico.solver.concurrent;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

import mb.statix.solver.IConstraint;
import mb.statix.solver.log.IDebugContext;
import mb.statix.solver.log.LazyDebugContext;
import mb.statix.taico.incremental.strategy.IncrementalStrategy;
import mb.statix.taico.incremental.strategy.NonIncrementalStrategy;
import mb.statix.taico.module.IModule;
import mb.statix.taico.solver.ASolverCoordinator;
import mb.statix.taico.solver.IMState;
import mb.statix.taico.solver.MSolverResult;
import mb.statix.taico.solver.ModuleSolver;

public class ConcurrentSolverCoordinator extends ASolverCoordinator {
    private final Map<ModuleSolver, SolverRunnable> solvers = Collections.synchronizedMap(new HashMap<>());
    private final Map<IModule, MSolverResult> results = Collections.synchronizedMap(new HashMap<>());
    private final ProgressCounter progressCounter = new ProgressCounter(this::onFinished);
    private final ExecutorService executors;
    
    private Consumer<MSolverResult> onFinished;
    private MSolverResult finalResult;
    
    public ConcurrentSolverCoordinator() {
        this(Executors.newWorkStealingPool());
    }
    
    public ConcurrentSolverCoordinator(ExecutorService executors) {
        this.executors = executors;
    }

    @Override
    public Map<IModule, MSolverResult> getResults() {
        return results;
    }
    
    @Override
    public Set<ModuleSolver> getSolvers() {
        return solvers.keySet();
    }
    
    @Override
    protected void init(IncrementalStrategy strategy, IMState rootState, Iterable<IConstraint> constraints, IDebugContext debug) {
        this.finalResult = null;
        super.init(strategy, rootState, constraints, debug);
    }
    
    @Override
    public void addSolver(ModuleSolver solver) {
        SolverRunnable runner = new SolverRunnable(solver, executors::submit, progressCounter, this::finishSolver, this::finishSolver);
        solver.getStore().setStoreObserver(store -> runner.notifyOfWork());
        solvers.put(solver, runner);
        runner.schedule();
    }
    
    private void finishSolver(ModuleSolver solver) {
        synchronized (solvers) {
            if (solvers.remove(solver) == null) {
                debug.warn("[" + solver.getOwner() + "] FinishSolver: ignoring, solver already removed");
                return;
            }
        }
        
        results.put(solver.getOwner(), solver.finishSolver());
    }
    
    @Override
    public MSolverResult solve(IMState state, Iterable<IConstraint> constraints, IDebugContext debug) throws InterruptedException {
        solveAsync(state, constraints, debug, null);
        awaitCompletion();
        return finalResult;
    }
    
    @Override
    public void solveAsync(IMState state, Iterable<IConstraint> constraints, IDebugContext debug, Consumer<MSolverResult> onFinished) {
        this.onFinished = onFinished;
        init(new NonIncrementalStrategy(), state, constraints, debug);
        addSolver(root);
    }
    
    @Override
    protected void scheduleModules(Map<IModule, Set<IConstraint>> modules) {
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
    private MSolverResult finishSolving(IDebugContext debug) {
        LazyDebugContext lazyDebug = new LazyDebugContext(debug);
        //If we end up here, none of the solvers is still able to make progress
        if (solvers.isEmpty()) {
            //All solvers are done!
            lazyDebug.info("All solvers finished successfully!");
        } else {
            lazyDebug.warn("Solving failed, {} unusuccessful solvers: ", solvers.size());
            IDebugContext sub = lazyDebug.subContext();
            for (ModuleSolver solver : getSolvers()) {
                sub.warn(solver.getOwner().getId());
                
                results.put(solver.getOwner(), solver.finishSolver());
            }
            
            solvers.clear();
        }
        logDebugInfo(lazyDebug);
        lazyDebug.commit();
        
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
        return finalResult;
    }

    @Override
    protected void runToCompletion() throws InterruptedException {
        awaitCompletion();
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
