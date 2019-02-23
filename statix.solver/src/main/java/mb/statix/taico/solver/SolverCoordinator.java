package mb.statix.taico.solver;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.metaborg.util.log.Level;

import mb.statix.solver.Delay;
import mb.statix.solver.IConstraint;
import mb.statix.solver.SolverResult;
import mb.statix.solver.State;
import mb.statix.solver.log.IDebugContext;
import mb.statix.solver.log.LazyDebugContext;
import mb.statix.taico.module.IModule;

public class SolverCoordinator {
    private final Set<ModuleSolver> solvers = Collections.synchronizedSet(new HashSet<>());
    private final Map<IModule, SolverResult> results = Collections.synchronizedMap(new HashMap<>());
    private ModuleSolver root;
    private MState rootState;
    
    public SolverCoordinator() {}
    
    public SolverResult solve(MState state, Iterable<IConstraint> constraints, IDebugContext debug)
        throws InterruptedException {
        rootState = state;
        root = ModuleSolver.topLevelSolver(state, constraints, debug);
        solvers.add(root);
        
        boolean anyProgress = true;
        ModuleSolver progressed = root; //The solver that has progressed, or null if multiple
        outer: while (anyProgress && !solvers.isEmpty()) {
            debug.log(Level.Trace, "[Coordinator] Begin of main loop");
            anyProgress = false;
            
            //Ensure that changes are not reflected this run
            
            ModuleSolver[] tempSolvers = solvers.toArray(new ModuleSolver[0]);
            for (ModuleSolver solver : tempSolvers) {
                debug.log(Level.Trace, "[Coordinator] Checking solver for module {}", solver.getOwner().getId());
                //If this solver is done, store its result and continue.
                if (solver.isDone()) {
                    debug.log(Level.Debug, "[Coordinator] [{}] done, removing...", solver.getOwner().getId());
                    solvers.remove(solver);
                    results.put(solver.getOwner(), solver.finishSolver());
                    continue;
                }
                debug.log(Level.Trace, "[Coordinator] [{}] is not done", solver.getOwner().getId());
                
                //TODO Improve the mechanism of delaying and "someone progresses, so just redo all"
                //If this solver is not the only solver that has made progress last round, then inform it that someone else has made progress.
                if (solver != progressed) {
                    debug.log(Level.Debug, "[Coordinator] [{}] informed of external progress by {}", solver.getOwner().getId(), progressed);
                    solver.externalProgress();
                } else {
                    debug.log(Level.Debug, "[Coordinator] [{}] is the only solver who made progress last round", solver.getOwner().getId());
                }
                
                debug.log(Level.Debug, "[Coordinator] [{}] going to try solve a step", solver.getOwner().getId());
                //If any progress can be made, store that information
                if (solver.solveStep()) {
                    debug.log(Level.Debug, "[Coordinator] [{}] solved one step", solver.getOwner().getId());
                    if (anyProgress) {
                        progressed = null;
                    } else {
                        anyProgress = true;
                        progressed = solver;
                    }
                    
                    //Solve this solver as far as possible
                    while (solver.solveStep());
                } else {
                    debug.log(Level.Debug, "[Coordinator] [{}] unable to solve a step", solver.getOwner().getId());
                }
                
                
                if (solver.hasFailed()) {
                    debug.log(Level.Debug, "[Coordinator] [{}] failed", solver.getOwner().getId());
                    break outer;
                }
            }
            
            debug.log(Level.Trace, "[Coordinator] end of main loop");
        }
        
        //If we end up here, none of the solvers is still able to make progress
        if (solvers.isEmpty()) {
            //All solvers are done!
            debug.info("[Coordinator] All solvers finished successfully!");
            
            //return results.get(state.owner());
        } else {
            LazyDebugContext lazyDebug = new LazyDebugContext(debug);
            lazyDebug.warn("[Coordinator] Solving failed, {} unusuccessful solvers: ", solvers.size());
            IDebugContext sub = lazyDebug.subContext();
            for (ModuleSolver solver : solvers) {
                sub.warn(solver.getOwner().getId());
            }
            
            debug(lazyDebug);
            
            //Finish all remaining solvers
            for (ModuleSolver solver : solvers) {
                results.put(solver.getOwner(), solver.finishSolver());
            }
            solvers.clear();
            
            //Log all the queued messages
            lazyDebug.commit();
        }
        
        SolverResult result = aggregateResults();
        
        return result;
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
        return SolverResult.of(State.of(rootState.spec()), new MCompleteness(), errors, delays);
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
