package mb.statix.taico.solver;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ForkJoinPool;

import mb.statix.solver.Completeness;
import mb.statix.solver.IConstraint;
import mb.statix.solver.State;
import mb.statix.solver.log.IDebugContext;

public class ConcurrentSolver {
    private Map<ModuleSolver, Boolean> solvers = new HashMap<>();
    private ExecutorService executor = pool();
    
    public ConcurrentSolver() {}
    
    public void addSolver(ModuleSolver solver, State state, Iterable<IConstraint> constraints, Completeness completeness, IDebugContext debug) {
        if (solvers.putIfAbsent(solver, false) != null) {
            throw new IllegalStateException("Solver " + solver + " is already in the pool!");
        }
        
        //TODO Taico is resubmitting necessary?
        executor.submit(solver);
    }
    
    private static final ExecutorService pool() {
        return new ForkJoinPool(
                Runtime.getRuntime().availableProcessors(),
                ForkJoinPool.defaultForkJoinWorkerThreadFactory,
                (t, e) -> {
                    System.err.println("Uncaught exception in execution of solver: " + e.getMessage());
                    e.printStackTrace();
                },
                true);
    }
}
