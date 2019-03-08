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
import mb.statix.solver.log.IDebugContext;
import mb.statix.solver.log.LazyDebugContext;
import mb.statix.solver.log.PrefixedDebugContext;
import mb.statix.taico.module.IModule;

public class SolverCoordinator {
    private final Set<ModuleSolver> solvers = Collections.synchronizedSet(new HashSet<>());
    private final Map<IModule, MSolverResult> results = Collections.synchronizedMap(new HashMap<>());
    private ModuleSolver root;
    private MState rootState;
    
    public SolverCoordinator() {}
    
    public MSolverResult solve(MState state, Iterable<IConstraint> constraints, IDebugContext debug)
        throws InterruptedException {
        rootState = state;
        root = ModuleSolver.topLevelSolver(state, constraints, debug);
        solvers.add(root);
        
        PrefixedDebugContext cdebug = new PrefixedDebugContext("Coordinator", debug);
        
        boolean anyProgress = true;
        ModuleSolver progressed = root; //The solver that has progressed, or null if multiple
        outer: while (anyProgress && !solvers.isEmpty()) {
            cdebug.log(Level.Trace, "Begin of main loop");
            anyProgress = false;
            
            //Ensure that changes are not reflected this run
            
            ModuleSolver[] tempSolvers = solvers.toArray(new ModuleSolver[0]);
            for (ModuleSolver solver : tempSolvers) {
                cdebug.log(Level.Trace, "Checking solver for module {}", solver.getOwner().getId());
                //If this solver is done, store its result and continue.
                if (solver.isDone()) {
                    cdebug.log(Level.Debug, "[{}] done, removing...", solver.getOwner().getId());
                    solvers.remove(solver);
                    results.put(solver.getOwner(), solver.finishSolver());
                    continue;
                }
                cdebug.log(Level.Trace, "[{}] is not done", solver.getOwner().getId());
                
                //TODO Improve the mechanism of delaying and "someone progresses, so just redo all"
                //If this solver is not the only solver that has made progress last round, then inform it that someone else has made progress.
                if (solver != progressed) {
                    cdebug.log(Level.Debug, "[{}] informed of external progress by {}", solver.getOwner().getId(), progressed);
                    solver.externalProgress();
                } else {
                    cdebug.log(Level.Debug, "[{}] is the only solver who made progress last round", solver.getOwner().getId());
                }
                
                cdebug.log(Level.Debug, "[{}] going to try solve a step", solver.getOwner().getId());
                //If any progress can be made, store that information
                if (solver.solveStep()) {
                    cdebug.log(Level.Debug, "[{}] solved one step", solver.getOwner().getId());
                    if (anyProgress) {
                        progressed = null;
                    } else {
                        anyProgress = true;
                        progressed = solver;
                    }
                    
                    //Solve this solver as far as possible
                    while (solver.solveStep());
                } else {
                    cdebug.log(Level.Debug, "[{}] unable to solve a step", solver.getOwner().getId());
                }
                
                
                if (solver.hasFailed()) {
                    cdebug.log(Level.Debug, "[{}] failed", solver.getOwner().getId());
                    break outer;
                }
            }
            
            cdebug.log(Level.Trace, "end of main loop");
        }
        
        //If we end up here, none of the solvers is still able to make progress
        if (solvers.isEmpty()) {
            //All solvers are done!
            cdebug.info("All solvers finished successfully!");
            
            //return results.get(state.owner());
        } else {
            LazyDebugContext lazyDebug = new LazyDebugContext(cdebug);
            lazyDebug.warn("Solving failed, {} unusuccessful solvers: ", solvers.size());
            IDebugContext sub = lazyDebug.subContext();
            for (ModuleSolver solver : solvers) {
                sub.warn(solver.getOwner().getId());
                
                results.put(solver.getOwner(), solver.finishSolver());
            }
            
            solvers.clear();
            
            //Output debug info
            debug(lazyDebug);
            
            //Log all the queued messages
            lazyDebug.commit();
        }
        
        MSolverResult result = aggregateResults();
        
        return result;
    }
    
    /**
     * Aggregates results of all the solvers into one SolverResult.
     * 
     * @return
     *      the aggregated results
     */
    public MSolverResult aggregateResults() {
        Set<IConstraint> errors = new LinkedHashSet<>();
        Map<IConstraint, Delay> delays = new LinkedHashMap<>();
        for (Entry<IModule, MSolverResult> result : results.entrySet()) {
            errors.addAll(result.getValue().errors());
            delays.putAll(result.getValue().delays());
        }
        return MSolverResult.of(rootState, rootState.solver().getCompleteness(), errors, delays);
    }
    
    /**
     * Logs debug output.
     * 
     * @param debug
     *      the debug context to log to
     */
    public void debug(IDebugContext debug) {
        debug.info("Debug output.");
        debug.info("Module hierarchy:");
        printModuleHierarchy(root.getOwner(), debug.subContext());
        
        LazyDebugContext success = new LazyDebugContext(debug.subContext());
        LazyDebugContext fail = new LazyDebugContext(debug.subContext());
        LazyDebugContext stuck = new LazyDebugContext(debug.subContext());
        LazyDebugContext failDetails = new LazyDebugContext(debug.subContext().subContext());
        LazyDebugContext stuckDetails = new LazyDebugContext(debug.subContext().subContext());
        
        for (Entry<IModule, MSolverResult> entry : results.entrySet()) {
            String id = entry.getKey().getId();
            if (entry.getValue().hasErrors()) {
                fail.info(id);
                failDetails.info("[{}] Failed constraints:", id);
                IDebugContext sub = failDetails.subContext();
                for (IConstraint c : entry.getValue().errors()) {
                    sub.info(c.toString());
                }
            } else if (entry.getValue().hasDelays()) {
                stuck.info(id);
                stuckDetails.info("[{}] Stuck constraints:", id);
                IDebugContext sub = stuckDetails.subContext();
                for (Entry<IConstraint, Delay> e : entry.getValue().delays().entrySet()) {
                    Delay delay = e.getValue();
                    if (!delay.vars().isEmpty()) {
                        sub.info("on vars {}: {}", delay.vars(), e.getKey());
                    } else if (!delay.criticalEdges().isEmpty()) {
                        sub.info("on edges {}: {}", delay.criticalEdges(), e.getKey());
                    } else {
                        sub.info("on unknown: {}", e.getKey());
                    }
                }
            } else {
               success.info(id); 
            }
        }
        
        debug.info("Finished modules:");
        success.commit();
        
        debug.info("Stuck modules:");
        stuck.commit();
        
        debug.info("Failed modules:");
        fail.commit();
        
        debug.info("Stuck output:");
        stuckDetails.commit();
        
        debug.info("Failed output:");
        failDetails.commit();
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
