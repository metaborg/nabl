package mb.statix.modular.solver.coordinator;

import static mb.statix.modular.util.TDebug.*;
import static mb.statix.modular.util.TPrettyPrinter.print;
import static mb.statix.modular.util.TPrettyPrinter.printModule;

import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Consumer;

import mb.statix.modular.incremental.changeset.IChangeSet;
import mb.statix.modular.incremental.strategy.IncrementalStrategy;
import mb.statix.modular.module.IModule;
import mb.statix.modular.solver.Context;
import mb.statix.modular.solver.MSolverResult;
import mb.statix.modular.solver.ModuleSolver;
import mb.statix.modular.solver.state.IMState;
import mb.statix.modular.util.TPrettyPrinter;
import mb.statix.solver.Delay;
import mb.statix.solver.IConstraint;
import mb.statix.solver.ISolverResult;
import mb.statix.solver.log.IDebugContext;
import mb.statix.solver.log.LazyDebugContext;

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
    public void setContext(Context context);
    
    /**
     * @return
     *      the context that this coordinator is currently coordinating
     */
    public Context getContext();
    
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
     * Logs a summary of solving.
     * 
     * @param debug
     *      the debug context to log to
     */
    public default void logSummary(IDebugContext debug) {
        LazyDebugContext success = new LazyDebugContext(debug.subContext());
        LazyDebugContext fail = new LazyDebugContext(debug.subContext());
        LazyDebugContext stuck = new LazyDebugContext(debug.subContext());
        LazyDebugContext failDetails = new LazyDebugContext(debug.subContext().subContext());
        LazyDebugContext stuckDetails = new LazyDebugContext(debug.subContext().subContext());
        int nrFailed = 0;
        int nrStuck = 0;
        int nrSuccess = 0;
        
        for (Entry<IModule, MSolverResult> entry : getResults().entrySet()) {
            String id = entry.getKey().getId();
            boolean a, b;
            if (a = entry.getValue().hasErrors()) {
                nrFailed++;
                fail.info(printModule(id));
                if (COORDINATOR_EXTENDED_SUMMARY) {
                    failDetails.info("[{}] Failed constraints:", id);
                    IDebugContext sub = failDetails.subContext();
                    for (IConstraint c : entry.getValue().errors()) {
                        sub.info(c.toString());
                    }
                }
            }
            if (b = entry.getValue().hasDelays()) {
                nrStuck++;
                stuck.info(printModule(id));
                if (COORDINATOR_EXTENDED_SUMMARY) {
                    stuckDetails.info("[{}] Stuck constraints:", id);
                    IDebugContext sub = stuckDetails.subContext();
                    for (Entry<IConstraint, Delay> e : entry.getValue().delays().entrySet()) {
                        Delay delay = e.getValue();
                        if (!delay.vars().isEmpty()) {
                            sub.info("on vars {}: {}", delay.vars(), e.getKey());
                        } else if (!delay.criticalEdges().isEmpty()) {
                            sub.info("on edges {}: {}", TPrettyPrinter.prettyPrint(delay.criticalEdges(), entry.getValue().unifier()), e.getKey());
                        } else {
                            sub.info("on unknown: {}", e.getKey());
                        }
                    }
                }
            }
            
            if (!a && !b) {
                nrSuccess++;
               success.info(printModule(id)); 
            }
        }
        
        if (nrFailed == 0 && nrStuck == 0) {
            debug.info("Solving completed successfully without failures.");
        } else {
            debug.info("Solving completed but {} modules failed and {} modules got stuck", nrFailed, nrStuck);
        }
        
        if (COORDINATOR_HIERARCHY) {
            debug.info("Module hierarchy:");
            printModuleHierarchy(getRootState().owner(), debug.subContext());
        }
        
        if (nrSuccess > 0) {
            debug.info("Successful modules:");
            success.commit();
        }
        
        if (nrStuck > 0) {
            debug.info("Stuck modules:");
            stuck.commit();
        }
        
        if (nrFailed > 0) {
            debug.info("Failed modules:");
            fail.commit();
        }
        
        if (COORDINATOR_EXTENDED_SUMMARY) {
            debug.info("Stuck constraints:");
            stuckDetails.commit();
            
            debug.info("Failed constraints:");
            failDetails.commit();
        }
        
        if (nrFailed == 0 && nrStuck == 0) {
            debug.info("Solving completed successfully without failures.");
        } else {
            debug.info("Solving completed but {} modules failed and {} modules got stuck", nrFailed, nrStuck);
        }
    }
    
    /**
     * Logs a short summary of the solving.
     * 
     * @param debug
     *      the debug context
     */
    public default void logShortSummary(IDebugContext debug) {
        int nrFailed = 0;
        int nrStuck = 0;
        for (MSolverResult result : getResults().values()) {
            if (result.hasErrors()) {
                nrFailed++;
            } else if (result.hasDelays()) {
                nrStuck++;
            }
        }
        
        if (nrFailed == 0 && nrStuck == 0) {
            debug.info("Solving completed successfully without failures.");
        } else {
            debug.info("Solving completed but {} modules failed and {} modules got stuck", nrFailed, nrStuck);
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
        context.info("{}: dependencies={}",
                printModule(module.getId()),
                print(module.getDependencyIds(), TPrettyPrinter::printModule));
        IDebugContext sub = context.subContext();
        for (IModule child : module.getChildren()) {
            printModuleHierarchy(child, sub);
        }
    }

    /**
     * Prevents execution of solvers until {@link #allowSolverStart()} is called.
     * 
     * @throws IllegalStateException
     *      If there are already solvers running.
     */
    public void preventSolverStart();
    
    /**
     * Allowes start execution of solvers.
     */
    public void allowSolverStart();

    public void wipe();
}
