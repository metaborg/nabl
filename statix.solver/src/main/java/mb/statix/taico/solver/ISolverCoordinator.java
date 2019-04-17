package mb.statix.taico.solver;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.Map.Entry;

import mb.statix.solver.Delay;
import mb.statix.solver.IConstraint;
import mb.statix.solver.log.IDebugContext;
import mb.statix.solver.log.LazyDebugContext;
import mb.statix.taico.module.IModule;

public interface ISolverCoordinator {
    /**
     * @return
     *      the root solver
     */
    public ModuleSolver getRootSolver();
    
    /**
     * @return
     *      the state of the root solver
     */
    public IMState getRootState();
    
    /**
     * @return
     *      a map containing all the solver results of solvers that have completed
     */
    public Map<IModule, MSolverResult> getResults();
    
    /**
     * @return
     *      a collection of all solvers
     */
    public Collection<ModuleSolver> getSolvers();
    
    /**
     * Adds the given solver to the collection of solvers.
     * 
     * @param solver
     *      the solver to add
     */
    public default void addSolver(ModuleSolver solver) {
        getSolvers().add(solver);
    }
    
    /**
     * Solves the given constraints in a modularized fashion.
     * 
     * @param state
     *      the state of the root module
     * @param constraints
     *      the constraints to solve
     * @param debug
     *      the debug context to log to
     * 
     * @return
     *      the aggregated result of solving
     * 
     * @throws InterruptedException
     *      If solving is interrupted.
     */
    public MSolverResult solve(IMState state, Iterable<IConstraint> constraints, IDebugContext debug) throws InterruptedException;
    
    /**
     * Solves the given constraints in a modularized fashion.
     * 
     * This method starts the solving process and then returns a future which can be used to 
     * 
     * @param state
     *      the state of the root module
     * @param constraints
     *      the constraints to solve
     * @param debug
     *      the debug context to log to
     * @param onFinished
     *      called whenever solving is finished
     * 
     * @return
     *      a future to get the solve result from
     * 
     * @throws UnsupportedOperationException
     *      If this solver does not support asynchronous solving.
     */
    public void solveAsync(IMState state, Iterable<IConstraint> constraints, IDebugContext debug, Consumer<MSolverResult> onFinished);
    
    /**
     * Aggregates results of all the solvers into one SolverResult.
     * 
     * @return
     *      the aggregated results
     */
    public default MSolverResult aggregateResults() {
        Set<IConstraint> errors = new LinkedHashSet<>();
        Map<IConstraint, Delay> delays = new LinkedHashMap<>();
        for (Entry<IModule, MSolverResult> result : getResults().entrySet()) {
            errors.addAll(result.getValue().errors());
            delays.putAll(result.getValue().delays());
        }
        return MSolverResult.of(getRootState(), errors, delays);
    }
    
    /**
     * Logs debug output.
     * 
     * @param debug
     *      the debug context to log to
     */
    public default void logDebugInfo(IDebugContext debug) {
        debug.info("Debug output.");
        debug.info("Module hierarchy:");
        printModuleHierarchy(getRootState().owner(), debug.subContext());
        
        LazyDebugContext success = new LazyDebugContext(debug.subContext());
        LazyDebugContext fail = new LazyDebugContext(debug.subContext());
        LazyDebugContext stuck = new LazyDebugContext(debug.subContext());
        LazyDebugContext failDetails = new LazyDebugContext(debug.subContext().subContext());
        LazyDebugContext stuckDetails = new LazyDebugContext(debug.subContext().subContext());
        
        for (Entry<IModule, MSolverResult> entry : getResults().entrySet()) {
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
    
    /**
     * Prints the module hierarchy to the given context, starting at the given module.
     * 
     * @param module
     *      the module to start at
     * @param context
     *      the debug context to print to
     */
    public default void printModuleHierarchy(IModule module, IDebugContext context) {
        context.info("{}: dependencies={}", module.getId(), module.getDependencies());
        IDebugContext sub = context.subContext();
        for (IModule child : module.getChildren()) {
            printModuleHierarchy(child, sub);
        }
    }
}
