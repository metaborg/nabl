package mb.statix.taico.solver.coordinator;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.metaborg.util.log.Level;

import mb.statix.solver.IConstraint;
import mb.statix.solver.ISolverResult;
import mb.statix.solver.log.IDebugContext;
import mb.statix.taico.incremental.changeset.IChangeSet;
import mb.statix.taico.incremental.strategy.IncrementalStrategy;
import mb.statix.taico.incremental.strategy.NonIncrementalStrategy;
import mb.statix.taico.module.IModule;
import mb.statix.taico.solver.MSolverResult;
import mb.statix.taico.solver.ModuleSolver;
import mb.statix.taico.solver.SolverContext;
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
        
        try {
            runToCompletion();
        } finally {
            deinit();
        }
        
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
    
    @Override
    public Map<String, ISolverResult> solve(IncrementalStrategy strategy, IChangeSet changeSet, IMState state, Map<String, IConstraint> constraints, IDebugContext debug)
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
     * Runs the solvers until completion.
     */
    @Override
    protected void runToCompletion() throws InterruptedException {
        boolean anyProgress = true;
        Set<ModuleSolver> failedSolvers = new HashSet<>();
        while (anyProgress && !solvers.isEmpty()) {
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
                //If any progress can be made, store that information
                SolverContext.setCurrentModule(solver.getOwner());
                if (solver.solveStep()) {
                    anyProgress = true;
                    
                    //Solve this solver as far as possible
                    while (solver.solveStep());
                }
                
                if (solver.hasFailed()) {
                    solvers.remove(solver);
                    failedSolvers.add(solver);
                    results.put(solver.getOwner(), solver.finishSolver());
                    continue;
                }
            }
            
            if (!anyProgress || solvers.isEmpty()) {
                if (context.getIncrementalManager().finishPhase()) {
                    this.debug.log(Level.Info, "Phase complete, starting new phase: {}" + context.getIncrementalManager().getPhase());
                    
                    //We need to readd solvers if they failed
                    solvers.addAll(failedSolvers);
                    failedSolvers.clear();
                    
                    //Signal that we want to do another round
                    anyProgress = true;
                }
            }
        }
        
        //If we end up here, none of the solvers is still able to make progress
        if (solvers.isEmpty()) {
            //All solvers are done!
            debug.info("All solvers finished successfully!");
        } else {
            System.err.println("Solving failed, " + solvers.size() + " unsuccessful solvers: " + solvers.stream().map(s -> s.getOwner().getId()).collect(Collectors.joining(", ")));
            debug.warn("Solving failed, {} unsuccessful solvers: ", solvers.size());
            IDebugContext sub = debug.subContext();
            for (ModuleSolver solver : solvers) {
                sub.warn(solver.getOwner().getId());
                
                results.put(solver.getOwner(), solver.finishSolver());
            }
            
            solvers.clear();
        }
        logDebugInfo(debug);
    }
}
