package mb.statix.taico.solver.concurrent;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import mb.statix.solver.IConstraint;
import mb.statix.solver.log.IDebugContext;
import mb.statix.solver.log.LazyDebugContext;
import mb.statix.taico.module.IModule;
import mb.statix.taico.solver.ISolverCoordinator;
import mb.statix.taico.solver.MSolverResult;
import mb.statix.taico.solver.MState;
import mb.statix.taico.solver.ModuleSolver;

public class ConcurrentSolverCoordinator implements ISolverCoordinator {
//    private final Map<ModuleSolver, Thread> solvers = Collections.synchronizedMap(new HashMap<>());
    private final Set<ModuleSolver> solvers = Collections.synchronizedSet(new HashSet<>());
    private final Map<IModule, MSolverResult> results = Collections.synchronizedMap(new HashMap<>());
    private final ExecutorService executors;
//    private AtomicInteger solving = new AtomicInteger(0);
    private AtomicInteger pending = new AtomicInteger(0);
//    private volatile boolean done;
    private ModuleSolver root;
    private MState rootState;
    
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
        return solvers;
    }
    
    @Override
    public void addSolver(ModuleSolver solver) {
        solver.getStore().setStoreObserver(store -> executeSolverCycle(solver));
        solvers.add(solver);
        executeSolverCycle(solver);
    }
    
    private void executeSolverCycle(ModuleSolver solver) {
        pending.incrementAndGet();
        executors.submit(() -> {
//            solving.incrementAndGet();
            try {
                if (solver.isDone() || solver.hasFailed()) {
                    finishSolver(solver);
                    return false;
                }
                
                System.err.println("[" + solver.getOwner() + "] Start of cycle");
                synchronized (solver) {
                    while (solver.solveStep());
                }
                System.err.println("[" + solver.getOwner() + "] End of cycle");
                return true;
            } finally {
//                solving.decrementAndGet();
                pending.decrementAndGet();
            }
        });
    }
    
    private void finishSolver(ModuleSolver solver) {
//        boolean lastOne;
        synchronized (solvers) {
            if (!solvers.remove(solver)) {
                System.err.println("[" + solver.getOwner() + "] FinishSolver: ignoring, solver already removed");
                return;
            }
            
//            lastOne = solvers.isEmpty(); 
        }
        
        results.put(solver.getOwner(), solver.finishSolver());
        
        //TODO Check pending here, if pending is 1, then we can safely complete the solving process.
        
        //If we are the last solver, then set done to true
//        if (lastOne) this.done = true;
        
    }
    
    @Override
    public MSolverResult solve(MState state, Iterable<IConstraint> constraints, IDebugContext debug) throws InterruptedException {
        rootState = state;
        root = ModuleSolver.topLevelSolver(state, constraints, debug);
        addSolver(root);
        
        //TODO I Should use a better threading mechanism for the whole thing
//        while (!done) {
//            Thread.sleep(100L);
//            
//            //TODO Find a better way to detect stuckness
//            for (int i = 0; solving.get() == 0 && i < 5; i++) {
//                System.err.println("No solvers are currently solving. Waiting...");
//                Thread.sleep(200L);
//            }
//        }
        
        while (pending.get() > 0) {
            Thread.sleep(100L);
        }
        
        System.err.println("Deducted that we are done solving");
        
        return finishSolving(debug);
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
            for (ModuleSolver solver : solvers) {
                sub.warn(solver.getOwner().getId());
                
                results.put(solver.getOwner(), solver.finishSolver());
            }
            
            solvers.clear();
        }
        logDebugInfo(lazyDebug);
        lazyDebug.commit();
        
        return aggregateResults();
    }

    @Override
    public Future<MSolverResult> solveAsync(MState state, Iterable<IConstraint> constraints, IDebugContext debug) {
        throw new UnsupportedOperationException();
    }
}
