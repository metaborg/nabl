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
import mb.statix.solver.log.PrefixedDebugContext;
import mb.statix.taico.module.IModule;
import mb.statix.taico.solver.ISolverCoordinator;
import mb.statix.taico.solver.MSolverResult;
import mb.statix.taico.solver.MState;
import mb.statix.taico.solver.ModuleSolver;

public class ConcurrentSolverCoordinator implements ISolverCoordinator {
    private final Map<ModuleSolver, SolverRunnable> solvers = Collections.synchronizedMap(new HashMap<>());
    private final Map<IModule, MSolverResult> results = Collections.synchronizedMap(new HashMap<>());
    private final ProgressCounter progressCounter = new ProgressCounter(this::onFinished);
    private final ExecutorService executors;
    
    private IDebugContext debug;
    private ModuleSolver root;
    private MState rootState;
    private Consumer<MSolverResult> onFinished;
    private MSolverResult finalResult;
    
    public ConcurrentSolverCoordinator() {
        this(Executors.newWorkStealingPool());
    }
    
    public ConcurrentSolverCoordinator(ExecutorService executors) {
        this.executors = executors;
    }
    
    @Override
    public ModuleSolver getRootSolver() {
        return root;
    }
    
    @Override
    public MState getRootState() {
        return rootState;
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
    public MSolverResult solve(MState state, Iterable<IConstraint> constraints, IDebugContext debug) throws InterruptedException {
        solveAsync(state, constraints, debug, null);

        synchronized (this) {
            while (finalResult == null) {
                wait();
            }
        }
        return finalResult;
    }
    
    @Override
    public void solveAsync(MState state, Iterable<IConstraint> constraints, IDebugContext debug, Consumer<MSolverResult> onFinished) {
        this.debug = new PrefixedDebugContext("Coordinator", debug);
        this.onFinished = onFinished;
        this.rootState = state;
        this.root = ModuleSolver.topLevelSolver(state, constraints, debug);
        addSolver(root);
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
}
