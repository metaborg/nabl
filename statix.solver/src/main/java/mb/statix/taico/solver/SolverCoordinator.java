package mb.statix.taico.solver;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import org.metaborg.util.log.Level;

import mb.statix.solver.IConstraint;
import mb.statix.solver.log.IDebugContext;
import mb.statix.solver.log.LazyDebugContext;
import mb.statix.taico.incremental.strategy.NonIncrementalStrategy;
import mb.statix.taico.module.IModule;

public class SolverCoordinator extends ASolverCoordinator {
    protected final Set<ModuleSolver> solvers = Collections.synchronizedSet(new HashSet<>());
    protected final Map<IModule, MSolverResult> results = Collections.synchronizedMap(new HashMap<>());
    
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
        
        runToCompletion();
        
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
            }
            
            onFinished.accept(aggregateResults());
        }).start();
    }
    
    /**
     * Runs the solvers until completion.
     */
    @Override
    protected void runToCompletion() throws InterruptedException {
        boolean anyProgress = true;
        int complete = 0;
        int failed = 0;
        while (anyProgress && !solvers.isEmpty()) {
            anyProgress = false;
            
            //Ensure that changes are not reflected this run
            ModuleSolver[] tempSolvers = solvers.toArray(new ModuleSolver[0]);
            int solverI = 0;
            for (ModuleSolver solver : tempSolvers) {
                solverI++;
                if (solverI % 10 == 0) {
                    this.debug.info("Solvers in run: " + solverI + "/" + tempSolvers.length + " (C" + complete + "/F" + failed + ")");
                }
                //If this solver is done, store its result and continue.
                if (solver.isDone()) {
                    complete++;
                    this.debug.log(Level.Debug, "[{}] done, removing...", solver.getOwner().getId());
                    solvers.remove(solver);
                    results.put(solver.getOwner(), solver.finishSolver());
                    continue;
                }
                //If any progress can be made, store that information
                SolverContext.setCurrentModule(solver.getOwner());
                if (solver.solveStep()) {
                    this.debug.log(Level.Debug, "[{}] solved one step", solver.getOwner().getId());
                    anyProgress = true;
                    
                    //Solve this solver as far as possible
                    while (solver.solveStep());
                } else {
                    this.debug.log(Level.Debug, "[{}] unable to solve a step", solver.getOwner().getId());
                }
                
                if (solver.hasFailed()) {
                    failed++;
                    this.debug.log(Level.Debug, "[{}] failed, removing...", solver.getOwner().getId());
                    solvers.remove(solver);
                    results.put(solver.getOwner(), solver.finishSolver());
                    continue;
                }
            }
        }
        
        LazyDebugContext lazyDebug = new LazyDebugContext(this.debug);
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
    }
}