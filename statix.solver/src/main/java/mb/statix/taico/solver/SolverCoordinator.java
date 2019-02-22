package mb.statix.taico.solver;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.google.common.collect.Sets;

import mb.statix.solver.Delay;
import mb.statix.solver.IConstraint;
import mb.statix.solver.SolverResult;
import mb.statix.solver.log.IDebugContext;
import mb.statix.solver.log.LazyDebugContext;
import mb.statix.taico.module.IModule;

public class SolverCoordinator {
    private final Set<ModuleSolver> solvers = Sets.newConcurrentHashSet();
    private final Map<IModule, SolverResult> results = Collections.synchronizedMap(new HashMap<>());
    private ModuleSolver root;
    
    public SolverCoordinator() {}
    
    public SolverResult solve(MState state, Iterable<IConstraint> constraints, IDebugContext debug)
        throws InterruptedException {
        
        root = ModuleSolver.topLevelSolver(state, constraints, debug);
        solvers.add(root);
        
        boolean anyProgress = true;
        ModuleSolver progressed = root; //The solver that has progressed, or null if multiple
        outer: while (anyProgress) {
            anyProgress = false;
            
            //Ensure that changes are not reflected this run
            for (ModuleSolver solver : solvers.toArray(new ModuleSolver[0])) {
                //If this solver is done, store its result and continue.
                if (solver.isDone()) {
                    solvers.remove(solver);
                    results.put(solver.getOwner(), solver.finishSolver());
                    continue;
                }
                
                //TODO Improve the mechanism of delaying and "someone progresses, so just redo all"
                //If this solver is not the only solver that has made progress last round, then inform it that someone else has made progress.
                if (solver != progressed) solver.externalProgress();
                
                //If any progress can be made, store that information
                if (solver.solveStep()) {
                    if (anyProgress) {
                        progressed = null;
                    } else {
                        anyProgress = true;
                        progressed = solver;
                    }
                }
                
                //Solve this solver as far as possible
                while (solver.solveStep());
                
                if (solver.hasFailed()) break outer;
            }
        }
        
        //If we end up here, none of the solvers is still able to make progress
        if (solvers.isEmpty()) {
            //All solvers are done!
            debug.info("[Coordinator] All solvers finished successfully!");
            
            //return results.get(state.owner());
        } else {
            debug.warn("[Coordinator] Solving failed, {} unusuccessful solvers: ", solvers.size());
            IDebugContext sub = debug.subContext();
            for (ModuleSolver solver : solvers) {
                sub.warn(solver.getOwner().getId());
            }
            
            debug(debug);
            
            //Finish all remaining solvers
            for (ModuleSolver solver : solvers) {
                results.put(solver.getOwner(), solver.finishSolver());
            }
            solvers.clear();
        }
        
        return aggregateResults();
    }
    
    /**
     * Aggregates results of all the solvers into one SolverResult.
     * 
     * @return
     *      the aggregated results
     */
    public SolverResult aggregateResults() {
        Set<IConstraint> errors = new LinkedHashSet<>();
        Map<IConstraint, Delay> delays = new LinkedHashMap<>();
        for (Entry<IModule, SolverResult> result : results.entrySet()) {
            errors.addAll(result.getValue().errors());
            delays.putAll(result.getValue().delays());
        }
        return SolverResult.of(null, null, errors, delays);
    }
    
    /**
     * Logs debug output.
     * 
     * @param debug
     *      the debug context to log to
     */
    public void debug(IDebugContext debug) {
        debug.info("[Coordinator] Debug output.");
        debug.info("[Coordinator] Module hierarchy:");
        printModuleHierarchy(root.getOwner(), debug.subContext());
        
        debug.info("[Coordinator] Finished modules:");
        IDebugContext sub = debug.subContext();
        for (Entry<IModule, SolverResult> entry : results.entrySet()) {
            sub.info(entry.getKey().getId());
        }
        
        LazyDebugContext fail = new LazyDebugContext(debug.subContext());
        LazyDebugContext stuck = new LazyDebugContext(debug.subContext());
        for (ModuleSolver solver : solvers) {
            if (solver.hasFailed()) {
                fail.info(solver.getOwner().getId());
            } else if (solver.isStuck()) {
                stuck.info(solver.getOwner().getId());
            }
        }
        
        debug.info("[Coordinator] Stuck modules:");
        stuck.commit();
        
        debug.info("[Coordinator] Failed modules:");
        fail.commit();
    }
    
    private void printModuleHierarchy(IModule module, IDebugContext context) {
        context.info("{}", module.getId());
        IDebugContext sub = context.subContext();
        for (IModule child : module.getChildren()) {
            printModuleHierarchy(child, sub);
        }
    }
    
    public void addSolver(ModuleSolver solver) {
        solvers.add(solver);
    }
}
