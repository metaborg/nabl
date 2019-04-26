package mb.statix.taico.solver;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.Consumer;

import mb.nabl2.terms.ITerm;
import mb.statix.scopegraph.terms.Scope;
import mb.statix.solver.Delay;
import mb.statix.solver.IConstraint;
import mb.statix.solver.ISolverResult;
import mb.statix.solver.constraint.CUser;
import mb.statix.solver.log.IDebugContext;
import mb.statix.solver.log.LazyDebugContext;
import mb.statix.solver.log.PrefixedDebugContext;
import mb.statix.taico.incremental.IChangeSet;
import mb.statix.taico.incremental.strategy.IncrementalStrategy;
import mb.statix.taico.module.IModule;
import mb.statix.taico.module.ModuleManager;
import mb.statix.taico.scopegraph.IOwnableScope;
import mb.statix.taico.scopegraph.OwnableScope;

public abstract class ASolverCoordinator {
    protected ModuleSolver root;
    protected IMState rootState;
    
    protected IncrementalStrategy strategy;
    protected IDebugContext debug;
    
    /**
     * @return
     *      the root solver
     */
    public ModuleSolver getRootSolver() {
        return root;
    }
    
    /**
     * @return
     *      the state of the root solver
     */
    public IMState getRootState() {
        return rootState;
    }
    
    /**
     * @return
     *      a map containing all the solver results of solvers that have completed
     */
    public abstract Map<IModule, MSolverResult> getResults();
    
    /**
     * @return
     *      a collection of all solvers
     */
    public abstract Collection<ModuleSolver> getSolvers();
    
    /**
     * Adds the given solver to the collection of solvers.
     * 
     * @param solver
     *      the solver to add
     */
    public abstract void addSolver(ModuleSolver solver);
    
    /**
     * Initializes the solver coordinator with the given root state and constraints.
     * 
     * @param strategy
     *      the incremental strategy
     * @param rootState
     *      the root state
     * @param constraints
     *      the constraints of the root module
     * @param debug
     *      the debug context
     */
    protected void init(IncrementalStrategy strategy, IMState rootState, Iterable<IConstraint> constraints, IDebugContext debug) {
        this.debug = new PrefixedDebugContext("Coordinator", debug);
        this.rootState = rootState;
        this.rootState.setCoordinator(this);
        this.root = ModuleSolver.topLevelSolver(rootState, constraints, debug);
        this.strategy = strategy;
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
    public abstract MSolverResult solve(IMState state, Iterable<IConstraint> constraints, IDebugContext debug) throws InterruptedException;
    
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
    public abstract void solveAsync(IMState state, Iterable<IConstraint> constraints, IDebugContext debug, Consumer<MSolverResult> onFinished);
    
    
    public Map<String, ISolverResult> solve(IncrementalStrategy strategy, IChangeSet changeSet, IMState state, Map<String, Set<IConstraint>> constraints, IDebugContext debug)
            throws InterruptedException {
        init(strategy, state, Collections.emptyList(), debug);
        
        Map<IModule, Set<IConstraint>> modules = createModules(constraints);
        System.err.println("Clearing dirty modules");
        strategy.clearDirtyModules(changeSet, state.manager());
        //Recreating modules
        recreateModules(modules.keySet(), state.manager());
        System.err.println("Scheduling");
        scheduleModules(modules);
        
        runToCompletion();
        
        return collectResults(modules.keySet());
    }
    
    /**
     * Resets modules that are marked for reanalysis.
     * 
     * @param modules
     *      the modules for which solvers will be created
     * @param manager
     *      the module manager
     */
    protected void recreateModules(Set<IModule> modules, ModuleManager manager) {
        for (IModule module : modules) {
            if (manager.getModule(module.getId()) != null) continue;
            
            module.reset(this, rootState.spec());
        }
    }

    /**
     * Creates / gets the modules from the given map from module name to constraints.
     * This method assumes that each module has exactly one constraint, which will be used
     * as initialization reason for the module.
     * 
     * A state is also created for each module.
     * 
     * @param moduleConstraints
     *      the map from module name to the initialization constraints
     * 
     * @return
     *      a map from module to initialization constraints
     */
    protected Map<IModule, Set<IConstraint>> createModules(Map<String, Set<IConstraint>> moduleConstraints) {
        IModule rootOwner = root.getOwner();
        Map<IModule, Set<IConstraint>> modules = new HashMap<>();
        for (Entry<String, Set<IConstraint>> entry : moduleConstraints.entrySet()) {
            String childName = entry.getKey();
            if (entry.getValue().size() > 1) {
                throw new IllegalArgumentException("Module " + childName + " has more than one initialization constraint: " + entry.getValue());
            }
            
            //Retrieve the child
            IModule child;
            if (entry.getValue().isEmpty()) {
                //Scope substitution does not have to occur here, since the global scope remains constant.
                //If there is no constraint available, use the initialization constraint for the child
                child = rootOwner.getChildAndAdd(childName);
                if (child != null) entry.setValue(Collections.singleton(child.getInitialization()));
            } else {
                IConstraint initConstraint = null;
                List<IOwnableScope> scopes = new ArrayList<>();
                for (IConstraint constraint : entry.getValue()) {
                    initConstraint = constraint;
                    if (!(constraint instanceof CUser)) break;
                    scopes = getScopes(rootState.manager(), (CUser) constraint);
                }
                
                child = rootOwner.createOrGetChild(childName, scopes, initConstraint);
            }
            
            if (child == null) throw new IllegalStateException("Child " + childName + " could not be found!");
            
            new MState(rootState.manager(), this, child, rootState.spec());
            modules.put(child, entry.getValue());
        }
        return modules;
    }
    
    /**
     * Determines the scopes in the arguments of the given CUser constraint.
     * 
     * @param manager
     *      the manager to get modules from
     * @param user
     *      the constraint
     * 
     * @return
     *      the list of scopes in the given constraint
     */
    private List<IOwnableScope> getScopes(ModuleManager manager, CUser user) {
        List<IOwnableScope> scopes = new ArrayList<>();
        for (ITerm term : user.args()) {
            Scope scope = Scope.matcher().match(term).orElse(null);
            if (scope != null) scopes.add(OwnableScope.fromScope(manager, scope));
        }
        return scopes;
    }
    
    /**
     * Schedules the given modules by creating solvers for them.
     * 
     * @param modules
     *      the modules to schedule
     */
    protected void scheduleModules(Map<IModule, Set<IConstraint>> modules) {
        for (Entry<IModule, Set<IConstraint>> entry : modules.entrySet()) {
            //childSolver sets the solver on the state and adds it
            root.childSolver(entry.getKey().getCurrentState(), entry.getValue());
        }
    }
    
    /**
     * Creates a map with all the results from module <b>NAME</b> to solver result.
     * The returned map will only contain results for modules present in the given collection. 
     * 
     * @param modules
     *      the modules to return results for
     * 
     * @return
     *      the results of the given modules
     */
    protected Map<String, ISolverResult> collectResults(Collection<IModule> modules) {
        Map<String, ISolverResult> results = new HashMap<>();
        for (Entry<IModule, MSolverResult> entry : getResults().entrySet()) {
            //Skip all modules that are not in the initial set.
            if (!modules.contains(entry.getKey())) continue;
            results.put(entry.getKey().getName(), entry.getValue());
        }
        return results;
    }
    
    /**
     * Makes sure this coordinator progresses until completion, and then returns.
     * 
     * @throws InterruptedException
     *      If the coordinator is interrupted while running.
     */
    protected abstract void runToCompletion() throws InterruptedException;
    
    /**
     * Aggregates results of all the solvers into one SolverResult.
     * 
     * @return
     *      the aggregated results
     */
    protected MSolverResult aggregateResults() {
        //TODO Instead of aggregating results, we should perhaps return the results of the top module?
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
    public void logDebugInfo(IDebugContext debug) {
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
    public void printModuleHierarchy(IModule module, IDebugContext context) {
        context.info("{}: dependencies={}", module.getId(), module.getDependencies());
        IDebugContext sub = context.subContext();
        for (IModule child : module.getChildren()) {
            printModuleHierarchy(child, sub);
        }
    }
}
