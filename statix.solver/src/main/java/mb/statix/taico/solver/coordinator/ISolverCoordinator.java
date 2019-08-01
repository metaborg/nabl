package mb.statix.taico.solver.coordinator;

import static mb.statix.taico.util.TDebug.*;

import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Consumer;

import mb.statix.solver.Delay;
import mb.statix.solver.IConstraint;
import mb.statix.solver.ISolverResult;
import mb.statix.solver.log.IDebugContext;
import mb.statix.solver.log.LazyDebugContext;
import mb.statix.taico.incremental.changeset.IChangeSet;
import mb.statix.taico.incremental.strategy.IncrementalStrategy;
import mb.statix.taico.module.IModule;
import mb.statix.taico.solver.MSolverResult;
import mb.statix.taico.solver.ModuleSolver;
import mb.statix.taico.solver.SolverContext;
import mb.statix.taico.solver.state.IMState;

/**
 * Interface to represent a coordinator for the solving process.
 * The coordinator directs the solving of the different modules, possibly in parallel.
 */
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
     *      the root module
     */
    public IModule getRootModule();
    
    /**
     * Sets the context that is reported to the SolverRunnables.
     * 
     * @param context
     *      the context
     */
    public void setContext(SolverContext context);
    
    /**
     * @return
     *      the context that this coordinator is currently coordinating
     */
    public SolverContext getContext();
    
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
    public void addSolver(ModuleSolver solver);
    
    /**
     * Solves the given constraints in a modularized fashion.
     * 
     * @param state
     *      the state of the root module
     * @param constraint
     *      the constraint to solve
     * @param debug
     *      the debug context to log to
     * 
     * @return
     *      the aggregated result of solving
     * 
     * @throws InterruptedException
     *      If solving is interrupted.
     */
    public MSolverResult solve(IMState state, IConstraint constraint, IDebugContext debug) throws InterruptedException;
    
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
    public void solveAsync(IMState state, IConstraint constraints, IDebugContext debug, Consumer<MSolverResult> onFinished);
    
    /**
     * Performs multi file solving. The given map of constraints contains the initialization
     * constraint for each file. Likewise, the output is a map with the solver result for each of
     * those files.
     * 
     * @param strategy
     *      the strategy to solve with
     * @param changeSet
     *      the change set
     * @param state
     *      the root state
     * @param constraints
     *      for each module (name), the constraint to solve
     * @param debug
     *      the debug context to log to
     * 
     * @return
     *      a map of module names to solver results
     * 
     * @throws InterruptedException
     */
    public Map<String, ISolverResult> solve(
            IncrementalStrategy strategy, IChangeSet changeSet, IMState state,
            Map<String, IConstraint> constraints, IDebugContext debug)
                    throws InterruptedException;
    
    /**
     * Logs debug output.
     * 
     * @param debug
     *      the debug context to log to
     */
    public default void logDebugInfo(IDebugContext debug) {
        if (!COORDINATOR_SUMMARY) return;
        
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
                if (COORDINATOR_EXTENDED_SUMMARY) {
                    failDetails.info("[{}] Failed constraints:", id);
                    IDebugContext sub = failDetails.subContext();
                    for (IConstraint c : entry.getValue().errors()) {
                        sub.info(c.toString());
                    }
                }
            } else if (entry.getValue().hasDelays()) {
                stuck.info(id);
                if (COORDINATOR_EXTENDED_SUMMARY) {
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
        
        if (COORDINATOR_EXTENDED_SUMMARY) {
            debug.info("Stuck output:");
            stuckDetails.commit();
            
            debug.info("Failed output:");
            failDetails.commit();
        }
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
        context.info("{}: dependencies={}", module.getId(), module.getDependencyIds());
        IDebugContext sub = context.subContext();
        for (IModule child : module.getChildren()) {
            printModuleHierarchy(child, sub);
        }
    }
}
