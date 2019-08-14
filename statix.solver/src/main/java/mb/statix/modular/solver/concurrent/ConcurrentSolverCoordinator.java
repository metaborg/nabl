package mb.statix.modular.solver.concurrent;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

import mb.statix.modular.incremental.strategy.IncrementalStrategy;
import mb.statix.modular.incremental.strategy.NonIncrementalStrategy;
import mb.statix.modular.module.IModule;
import mb.statix.modular.solver.MSolverResult;
import mb.statix.modular.solver.ModuleSolver;
import mb.statix.modular.solver.coordinator.ASolverCoordinator;
import mb.statix.modular.solver.coordinator.ISolverCoordinator;
import mb.statix.modular.solver.state.IMState;
import mb.statix.solver.IConstraint;
import mb.statix.solver.log.IDebugContext;

public class ConcurrentSolverCoordinator extends ASolverCoordinator {
    private final Map<ModuleSolver, SolverRunnable> solvers = Collections.synchronizedMap(new HashMap<>());
    private final Object resultsSync = new Object();
    private final ProgressCounter progressCounter = new ProgressCounter(this::onFinishPhase);
    protected final ExecutorService executors;
    private StuckDetector stuckDetector;
    
    private Consumer<MSolverResult> onFinished;
    private volatile MSolverResult finalResult;
    private volatile boolean paused;
    private volatile boolean roundDone;
    
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
        return context.getResults();
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
        SolverRunnable runner = new SolverRunnable(solver, executors::submit, progressCounter, this::finishSuccessfulSolver, this::finishFailedSolver);
        solver.getStore().setStoreObserver(store -> runner.notifyOfWork());
        solvers.put(solver, runner);
        if (!paused) runner.schedule();
    }
    
    /**
     * Called by the runnable of a solver whenever that solver finishes successfully.
     * 
     * @param solver
     *      the solver
     */
    private void finishSuccessfulSolver(ModuleSolver solver) {
        synchronized (solvers) {
            SolverRunnable runnable = solvers.remove(solver);
            if (runnable == null) {
                debug.warn("[{}] FinishSolver: ignoring, solver already removed", solver.getOwner().getId());
                return;
            }
        }
        
        synchronized (resultsSync) {
            context.addResult(solver.getOwner(), solver.finishSolver());
        }
    }
    
    /**
     * Called by the runnable of a solver whenever that solver finishes unsuccessfully.
     * 
     * @param solver
     *      the solver
     */
    private void finishFailedSolver(ModuleSolver solver) {
        synchronized (solvers) {
            SolverRunnable runnable = solvers.remove(solver);
            if (runnable == null) {
                debug.warn("[{}] FinishSolver: ignoring, solver already removed", solver.getOwner());
                return;
            }
        }
        
        synchronized (resultsSync) {
            context.addResult(solver.getOwner(), solver.finishSolver());
        }
    }
    
    @Override
    public MSolverResult solve(IMState state, IConstraint constraint, IDebugContext debug) throws InterruptedException {
        solveAsync(state, constraint, debug, null);
        runToCompletion();
        return finalResult;
    }
    
    @Override
    public void solveAsync(IMState state, IConstraint constraint, IDebugContext debug, Consumer<MSolverResult> onFinished) {
        this.onFinished = onFinished;
        init(new NonIncrementalStrategy(), state, constraint, debug);
        if (context.isInitPhase()) context.finishInitPhase();
        
        addSolver(root);
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
    
    /**
     * Called whenever the current phase is finished.
     */
    private void onFinishPhase() {
        try {
            System.out.println("COUNTER HAS INFORMED US THAT THE PHASE IS FINISHED :party:");
            roundDone = true;
            synchronized (this) {
                notifyAll();
            }
        } catch (Exception ex) {
            System.err.println("Uncaught exception in coordinator: ");
            ex.printStackTrace();
            throw ex;
        }
    }

    @Override
    protected boolean startNextPhase(Set<ModuleSolver> finishedSolvers, Set<ModuleSolver> failedSolvers,
            Set<ModuleSolver> stuckSolvers, Map<IModule, MSolverResult> results) {
        //TODO How to stop all the solvers that are still in "waiting" mode?
        preventSolverStart();
        if (!context.getIncrementalManager().finishPhase(finishedSolvers, failedSolvers, stuckSolvers, results)) return false;
        allowSolverStart();
        return true;
    }

    /**
     * Finishes the solving process. Should only be called after finishing a round.
     * 
     * @param debug
     *      the debug context to log to
     */
    @Override
    protected void finishSolving() {
        System.out.println("FinishSolving called");
        super.finishSolving();
        System.out.println("Super call done");
        
        try {
            finalResult = aggregateResults();
        } catch (Exception ex) {
            System.err.println("ERROR while aggregating results!");
            ex.printStackTrace();
        }
        
        System.out.println("Results aggregated");
        try {
            if (onFinished != null) {
                try {
                    onFinished.accept(finalResult);
                } catch (Throwable t) {
                    debug.error("On finished consumer threw an exception: {}", t.getMessage());
                }
            }
        } finally {
            //Notify any thread that is waiting for the final result that it has been obtained.
            synchronized (this) {
                notifyAll();
            }
        }
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
     */
    private synchronized void awaitCompletion() {
        try {
            while (true) {
                wait();
                
                if (!roundDone) continue;
                
                if (!finishPhase()) {
                    finishSolving();
                    break;
                }
                roundDone = false;
            }
        } catch (Exception ex) {
            failSolving(ex);
        }
    }
    
    @Override
    public void preventSolverStart() {
        if (paused) return;
        if (!solvers.isEmpty()) throw new IllegalStateException("It is only allowed to pause whenever no solvers are running yet!");
        paused = true;
    }
    
    @Override
    public void allowSolverStart() {
        if (!paused) return;
        synchronized (solvers) {
            for (SolverRunnable runnable : solvers.values()) {
                runnable.schedule();
            }
        }
        paused = false;
    }
}
