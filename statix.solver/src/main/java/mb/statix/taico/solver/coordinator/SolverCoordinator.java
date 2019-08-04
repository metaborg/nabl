package mb.statix.taico.solver.coordinator;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import mb.statix.solver.IConstraint;
import mb.statix.solver.log.IDebugContext;
import mb.statix.taico.incremental.strategy.NonIncrementalStrategy;
import mb.statix.taico.module.IModule;
import mb.statix.taico.solver.MSolverResult;
import mb.statix.taico.solver.ModuleSolver;
import mb.statix.taico.solver.state.IMState;

/**
 * A sequential solver coordinator. Modules are solved one after the other.
 * 
 * This coordinator uses a greedy solving process. That is, each module is solved as far as
 * possible before the coordinator moves on to the next module.
 */
public class SolverCoordinator extends ASolverCoordinator {
    protected final Set<ModuleSolver> solvers = new HashSet<>();
    protected final Map<IModule, MSolverResult> results = new HashMap<>();
    
    public SolverCoordinator() {}
    
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
        solvers.add(solver);
    }
    
    @Override
    public MSolverResult solve(IMState state, IConstraint constraint, IDebugContext debug)
        throws InterruptedException {
        init(new NonIncrementalStrategy(), state, constraint, debug);
        addSolver(root);
        
        if (context.isInitPhase()) context.finishInitPhase();
        runToCompletion();
        
        //TODO Does not perform a multi phase approach yet
        return aggregateResults();
    }
    
    @Override
    public void solveAsync(IMState state, IConstraint constraint, IDebugContext debug, Consumer<MSolverResult> onFinished) {
        init(new NonIncrementalStrategy(), state, constraint, debug);
        new Thread(() -> {
            try {
                runToCompletion();
            } catch (InterruptedException ex) {
                this.debug.error("Interrupted while solving!");
            } finally {
                deinit();
            }
            
            onFinished.accept(aggregateResults());
        }).start();
    }
    
    /**
     * Runs the solvers until completion.
     */
    @Override
    protected void runToCompletion() throws InterruptedException {
        try {
            solverLoop();
        } catch (Exception ex) {
            failSolving(ex);
            return;
        }
        
        finishSolving();
    }

    private void solverLoop() throws InterruptedException {
        boolean anyProgress = true;
        while (true) {
            anyProgress = false;
            
            //Ensure that changes are not reflected this run
            ModuleSolver[] tempSolvers = solvers.toArray(new ModuleSolver[0]);
            for (ModuleSolver solver : tempSolvers) {
                //If this solver is done, store its result and continue.
                if (solver.isDone()) {
                    solvers.remove(solver);
                    results.put(solver.getOwner(), solver.finishSolver());
                    continue;
                }
                
                //Check if we were interrupted
                if (Thread.interrupted()) throw new InterruptedException();
                
                //If any progress can be made, store that information
                if (solver.solveStep()) {
                    anyProgress = true;
                    
                    //Solve this solver as far as possible
                    while (solver.solveStep());
                }
                
                if (solver.hasFailed()) {
                    solvers.remove(solver);
                    results.put(solver.getOwner(), solver.finishSolver());
                    continue;
                }
            }
            
            if (!anyProgress || solvers.isEmpty()) {
                if (!finishPhase()) {
                    break;
                }
            }
        }
    }
}
