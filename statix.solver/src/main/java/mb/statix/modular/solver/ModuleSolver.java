package mb.statix.modular.solver;

import static mb.statix.modular.util.TDebug.CONSTRAINT_SOLVING;
import static mb.statix.modular.util.TDebug.DEV_NULL;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.metaborg.util.log.Level;

import com.google.common.collect.ImmutableMap;

import mb.nabl2.terms.ITermVar;
import mb.nabl2.terms.unification.IUnifier;
import mb.nabl2.terms.unification.UnifierFormatter;
import mb.nabl2.util.TermFormatter;
import mb.statix.modular.module.IModule;
import mb.statix.modular.module.split.SplitModuleUtil;
import mb.statix.modular.scopegraph.reference.ModuleDelayException;
import mb.statix.modular.solver.completeness.RedirectingIncrementalCompleteness;
import mb.statix.modular.solver.concurrent.ConcurrentRedirectingIncrementalCompleteness;
import mb.statix.modular.solver.state.IMState;
import mb.statix.modular.solver.store.ModuleConstraintStore;
import mb.statix.modular.util.IOwnable;
import mb.statix.modular.util.TDebug;
import mb.statix.modular.util.TOptimizations;
import mb.statix.modular.util.TOverrides;
import mb.statix.solver.Delay;
import mb.statix.solver.IConstraint;
import mb.statix.solver.completeness.Completeness;
import mb.statix.solver.completeness.IsComplete;
import mb.statix.solver.log.FakeLazyDebugContext;
import mb.statix.solver.log.IDebugContext;
import mb.statix.solver.log.LazyDebugContext;
import mb.statix.solver.log.Log;
import mb.statix.solver.log.NullDebugContext;
import mb.statix.solver.log.PrefixedDebugContext;
import mb.statix.solver.persistent.Solver;

public class ModuleSolver implements IOwnable {
    private final IMState state;
    private Map<ITermVar, ITermVar> existentials = null;
    private final ModuleConstraintStore constraints;
    private final RedirectingIncrementalCompleteness completeness;
    private final PrefixedDebugContext debug;
    private final LazyDebugContext proxyDebug;
    private boolean separateSolver;
    private boolean init;
    private boolean splitCheck;
    private boolean noopSolver;
    private boolean topLevel;
    
    private IsComplete isComplete;
    
    // time log
    private final Map<Class<? extends IConstraint>, Long> successCount = new HashMap<>();
    private final Map<Class<? extends IConstraint>, Long> delayCount = new HashMap<>();

    // fixed point
    private final Set<IConstraint> failed = new HashSet<>();
    private final Log delayedLog = new Log();
    private int reductions = 0;
    private int delays = 0;
    
    /**
     * Creates a new top level solver.
     * <p>
     * <b>NOTE:</b> sets the solver of the given state to the created solver.
     * 
     * @param state
     *      the state to create the solver for
     * @param constraints
     *      the constraints that should be solved
     * @param debug
     *      the debug context
     * 
     * @return
     *      the created solver
     */
    public static ModuleSolver topLevelSolver(IMState state, IConstraint constraint, IDebugContext debug) {
        PrefixedDebugContext topDebug = new PrefixedDebugContext(state.owner().getId(), debug);
        ModuleSolver solver = new ModuleSolver(state, constraint, (s, l, st) -> true, topDebug, false);
        solver.topLevel = true;
        return solver;
    }
    
    /**
     * Creates a special solver for a library module. The returned solver does not have debug
     * output and functions like a stronger version of the noop solver.
     * <p>
     * This solver will not be initialized with the incremental manager, to avoid special behavior
     * that is unwanted for the library.
     * <p>
     * The solver is not added to the coordinator, since there will never be any solving to do for
     * this module, nor should it show up in the results of the solving process.
     * 
     * @param state
     *      the state of the module
     * 
     * @return
     *      the created solver
     */
    public static ModuleSolver librarySolver(IMState state) {
        PrefixedDebugContext debug = new PrefixedDebugContext(state.owner().getId(), new NullDebugContext());
        ModuleSolver solver = new ModuleSolver(state, null, (s, l, st) -> true, debug, false, false);
        solver.noopSolver = true;
        return solver;
    }
    
    private ModuleSolver(IMState state, IConstraint constraint, IsComplete _isComplete, PrefixedDebugContext debug, boolean separateSolver) {
        this(state, constraint, _isComplete, debug, separateSolver, true);
    }
    
    private ModuleSolver(IMState state, IConstraint constraint, IsComplete _isComplete, PrefixedDebugContext debug, boolean separateSolver, boolean initSolver) {
        this(state, constraint, _isComplete, debug, separateSolver, initSolver,
                TOverrides.CONCURRENT ? new ConcurrentRedirectingIncrementalCompleteness(state.owner().getId(), state.spec())
                                      : new RedirectingIncrementalCompleteness(state.owner().getId(), state.spec()));
    }
    
    private ModuleSolver(IMState state, IConstraint constraint, IsComplete _isComplete, PrefixedDebugContext debug, boolean separateSolver, boolean initSolver, RedirectingIncrementalCompleteness completeness) {
        final String owner = state.owner().getId();
        this.state = state;
        this.debug = debug;
        this.proxyDebug = new FakeLazyDebugContext(debug); //TODO Remove at some point
        this.separateSolver = separateSolver;
        this.splitCheck = separateSolver;
        
        //Create the constraint store and the completeness. Also add the initial constraints
        Iterable<IConstraint> constraintList = constraint == null ? Collections.emptyList() : Collections.singletonList(constraint);
        this.constraints = new ModuleConstraintStore(owner, constraintList, debug);
        this.completeness = completeness;
        this.completeness.addAll(constraintList, state.unifier());
        
        if (_isComplete == null) {
            this.isComplete = (s, l, u) -> this.completeness.isComplete(s, l, state.unifier());
        } else {
            this.isComplete = (s, l, u) -> this.completeness.isComplete(s, l, state.unifier()) && _isComplete.test(s, l, u);
        }

        state.setSolver(this);
        if (initSolver) Context.context().getIncrementalManager().initSolver(this);
    }
    
    /**
     * Creates a solver as a child of this solver.
     * <p>
     * <b>NOTE</b>: This method adds the created solver to the coordinator.<br>
     * <b>NOTE</b>: This method sets the solver of the given state to the created solver.
     * 
     * @param state
     *      the newly created state for this solver
     * @param constraints
     *      the constraints to solve
     * 
     * @return
     *      the new solver
     */
    public ModuleSolver childSolver(IMState state, IConstraint constraint) {
        String id = state.owner().getId();
        PrefixedDebugContext debug = this.debug.createSibling(id);
        ModuleSolver solver = new ModuleSolver(state, constraint, this.isComplete, debug, false);
        
        if (!SplitModuleUtil.isSplitModule(state.owner().getId())) {
            //This module should solve in restricted mode
            Context.context().getIncrementalManager().registerNonSplit(id);
        }
        this.state.coordinator().addSolver(solver);
        
        return solver;
    }
    
    /**
     * Creates a solver as a child of this solver, reusing the completeness from the given solver
     * if it is not null.
     * <p>
     * <b>NOTE</b>: This method adds the created solver to the coordinator.<br>
     * <b>NOTE</b>: This method sets the solver of the given state to the created solver.
     * 
     * @param state
     *      the newly created state for this solver
     * @param constraints
     *      the constraints to solve
     * @param oldSolver
     *      the old solver
     * 
     * @return
     *      the new solver
     */
    public ModuleSolver childSolver(IMState state, IConstraint constraint, @Nullable ModuleSolver oldSolver) {
        String id = state.owner().getId();
        PrefixedDebugContext debug = this.debug.createSibling(id);
        ModuleSolver solver;
        if (oldSolver == null) {
            solver = new ModuleSolver(state, constraint, this.isComplete, debug, false);
        } else {
            //Reuse the old completeness
            assert oldSolver.completeness.isInDelayMode() : "The old solver should have a completeness in delay mode! (" + id + ")";
            solver = new ModuleSolver(state, constraint, this.isComplete, debug, false, true, oldSolver.completeness);
            solver.completeness.switchDelayMode(false);
        }
        
        if (!SplitModuleUtil.isSplitModule(state.owner().getId())) {
            //This module should solve in restricted mode
            Context.context().getIncrementalManager().registerNonSplit(id);
        }
        this.state.coordinator().addSolver(solver);
        
        return solver;
    }
    
    /**
     * Creates a solver for the given state that does not do any solving.
     * 
     * @param state
     *      the state
     * 
     * @return
     *      the new solver
     */
    public ModuleSolver noopSolver(IMState state) {
        PrefixedDebugContext debug = this.debug.createSibling(state.owner().getId());
        ModuleSolver solver = new ModuleSolver(state, null, this.isComplete, debug, false);
        solver.noopSolver = true;
        //TODO Can it be avoided that the noopsolver has to be added to the coordinator?
        this.state.coordinator().addSolver(solver);
        return solver;
    }
    
    /**
     * Creates a solver for the given state that does not do any solving.
     * Uses the given result to add the delayed/failed constraints from.
     * 
     * @param state
     *      the state
     * @param result
     *      the result
     * 
     * @return
     *      the new solver
     */
    public ModuleSolver noopSolver(IMState state, MSolverResult result) {
        PrefixedDebugContext debug = this.debug.createSibling(state.owner().getId());
        ModuleSolver solver = new ModuleSolver(state, null, this.isComplete, debug, false);
        solver.noopSolver = true;
        solver.constraints.fillFromResult(result);
        solver.completeness.fillFromResult(result);
        solver.fillFailedFromResult(result);
        //TODO Can it be avoided that the noopsolver has to be added to the coordinator?
        this.state.coordinator().addSolver(solver);

        return solver;
    }
    
    /**
     * Fills failed constraints from the result.
     * 
     * @param result
     */
    private void fillFailedFromResult(MSolverResult result) {
        failed.addAll(result.errors());
    }
    
    /**
     * Cleans up this solver in order for it to be replaced with the given solver.
     * 
     * This cleans the completeness and transfers observers.
     */
    public void replaceWith(ModuleSolver replacement) {
        TDebug.DEV_OUT.info("Cleaning up solver of " + getOwner() + " for replacement solver");
        assert constraints.activeSize() == 0 : "Solver to be replaced has ACTIVE constraints!";
        
        //TODO It is possible that requests will never be answered because the variable in question no longer exists.
        constraints.transferAllObservers(replacement.constraints, replacement.state.unifier());
    }

    /**
     * Solves the given arguments separately from other solvers. Separate solvers are not allowed
     * to cross module boundaries. 
     * 
     * @param state
     *      a (delegating) state
     * @param constraints
     *      the constraints to solve
     * @param isComplete
     *      the isComplete completeness predicate
     * @param debug
     *      the debug context
     * 
     * @return
     *      the result
     * 
     * @throws InterruptedException
     *      If the solver is interrupted.
     * @throws UnsupportedOperationException
     *      If the solver crosses a module boundary.
     */
    public static MSolverResult solveSeparately(
            IMState state,
            IConstraint constraint,
            IsComplete isComplete,
            IDebugContext debug) throws InterruptedException {
        PrefixedDebugContext debug2 = new PrefixedDebugContext("", debug.subContext());
        ModuleSolver solver = new ModuleSolver(state, constraint, isComplete, debug2, true);
        
        state.setSolver(solver); //TODO sets the solver of this delegate state to the given solver
        while (solver.solveStep());
        return solver.finishSolver();
    }
    
    @Override
    public IModule getOwner() {
        return state.owner();
    }
    
    public ModuleConstraintStore getStore() {
        return constraints;
    }
    
    public IsComplete getOwnIsComplete() {
        return isComplete;
    }
    
    public RedirectingIncrementalCompleteness getCompleteness() {
        return completeness;
    }
    
    /**
     * 
     * @return
     *      true if this solver is only solving entails, false otherwise
     */
    public boolean isSeparateSolver() {
        return separateSolver;
    }
    
    /**
     * @return
     *      true if this solver is a no-op solver
     */
    public boolean isNoopSolver() {
        return noopSolver;
    }
    
    /**
     * @return
     *      true if this solver is a top level solver
     */
    public boolean isTopLevelSolver() {
        return topLevel;
    }
    
    /**
     * The solver is guaranteed to be done if it has no more constraints.
     * It should be able to be done even if there are child solvers still solving.
     * 
     * <p>NOTE: This method is not concurrency safe! The result is only correct if it is requested
     * by the thread currently executing the solver, or if there is no thread currently executing
     * the solver. Otherwise, there is a small window where a true result does not actually mean
     * that the solver is done.
     * 
     * @return
     *      true if this solver is done, false otherwise
     */
    public boolean isDone() {
        return constraints.isDone();
    }
    
    /**
     * @return
     *      true if this solver has failed constraints, false otherwise
     */
    public boolean hasFailed() {
        return !failed.isEmpty();
    }
    
    /**
     * @return
     *      all the failed constraints
     */
    public Set<IConstraint> getFailed() {
        return failed;
    }
    
    /**
     * Solves a single constraint. A false return value indicates that there are no constraints
     * that can currently be activated and that no constraint was solved.
     * 
     * @return
     *      true if another step is required, false otherwise
     * 
     * @throws InterruptedException
     *      If the solver is interrupted.
     */
    public boolean solveStep() throws InterruptedException {
        if (!init) {
            Context.context().getIncrementalManager().solverStart(this);
            init = true;
        }
        IConstraint constraint = constraints.remove();
        if (constraint == null) {
            if (!splitCheck) checkCreateSplitModule();
            return false;
        }
    
        IDebugContext subDebug = CONSTRAINT_SOLVING ? proxyDebug.subContext() : DEV_NULL;
        if(proxyDebug.isEnabled(Level.Info)) {
            // proxyDebug.info("Solving {}", constraint.toString(ModuleSolver.shallowTermFormatter(state.unifier().unrestricted())));
        }
        
        try {
            final Optional<MConstraintResult> maybeResult;
            maybeResult = solveConstraint(constraint, subDebug);
            addTime(constraint, 1, successCount, debug);
            
            reductions += 1;
            if(maybeResult.isPresent()) {
                final MConstraintResult result = maybeResult.get();
                if(existentials == null) {
                    existentials = result.existentials();
                }
                if(!result.constraints().isEmpty()) {
                    //Do not override the cause if one is set. This is important for rule tracking (scope identity)
                    final List<IConstraint> newConstaints = result.constraints().stream()
                            .map(c -> c.cause().isPresent() ? c : c.withCause(constraint)).collect(Collectors.toList());
                    if(subDebug.isEnabled(Level.Info)) {
                        subDebug.info("Simplified to {}", toString(newConstaints, state.unifier()));
                    }
                    constraints.addAll(newConstaints);
                    completeness.addAll(newConstaints, state.unifier());
                }
                //Only remove the solved constraint after new constraints are added (for concurrent atomicity)
                completeness.remove(constraint, state.unifier());
                
                //Activate constraints after updating the completeness
                completeness.updateAll(result.vars(), state.unifier());
                constraints.activateFromVars(result.vars(), subDebug);
                
                //If we do not use the observer mechanism for our own constraints, just activate all the edges that are potentially affected
                if (!TOptimizations.USE_OBSERVER_MECHANISM_FOR_SELF) constraints.activateFromEdges(Completeness.criticalEdges(constraint, state.spec(), state.unifier()), subDebug);
            } else {
                completeness.remove(constraint, state.unifier());
                subDebug.error("Failed");
                failed.add(constraint);
                if(proxyDebug.isRoot()) {
                    printTrace(constraint, state.unifier(), subDebug);
                } else {
                    proxyDebug.info("Break early because of errors.");
                    proxyDebug.commit();
                    return false;
                }
            }
            proxyDebug.commit();
        } catch(Delay d) {
            addTime(constraint, 1, delayCount, debug);
            if (!d.vars().isEmpty()) {
                subDebug.info("Delayed on " + d.vars());
            } else if (!d.criticalEdges().isEmpty()) {
                subDebug.info("Delayed on " + d.criticalEdges());
            }
            delayedLog.absorb(proxyDebug.copy());
            proxyDebug.commit();
            constraints.delay(constraint, d, state);
            delays += 1;
        } catch (Exception ex) {
            TDebug.DEV_OUT.info("FATAL: Exception encountered while solving!");
            ex.printStackTrace();
            return false;
        }
        return true;
    }
    
    private Optional<MConstraintResult> solveConstraint(IConstraint constraint, IDebugContext subDebug) throws InterruptedException, Delay {
        try {
            return constraint.solve(state, new MConstraintContext(isComplete, subDebug));
        } catch (ModuleDelayException ex) {
            throw Delay.ofModule(ex.getModule());
        }
    }
    
    private void checkCreateSplitModule() {
        splitCheck = true;
        if (!TOverrides.SPLIT_MODULES) return;
        
        //We are a split module ourselves, no need to split further
        String ownerId = getOwner().getId();
        if (SplitModuleUtil.isSplitModule(ownerId)) return;
        
        TDebug.DEV_OUT.info(getOwner() + " is performing split check...");
        String splitId = SplitModuleUtil.getSplitModuleId(ownerId);
        
        //TODO How to handle using old modules
        IModule splitCurrent = Context.context().getModuleManager().getModule(splitId);
        if (splitCurrent != null) {
            //a. The split already exists in the current context, update it
            TDebug.DEV_OUT.info("Split module " + splitId + " already exists in the current context, but was not created by the solver of the original module. State = " + splitCurrent.getTopCleanliness() + ". Updating...");
            SplitModuleUtil.updateSplitModule(splitCurrent);
        } else if ((splitCurrent = Context.context().getModuleUnchecked(splitId)) != null) {
            //TODO This scenario depends on the strategy. Will the strategy create a new one?
            TDebug.DEV_OUT.info("Split module " + splitId + " existed in the previous context, but not in the current");
            //b. The split already exists in the previous context: create a new one in the current context
            
            //Notify the incremental manager to make a decision over this split module. It might only be interested in the structure here
            if (Context.context().getIncrementalManager().createSplitModuleRequest(ownerId)) {
                TDebug.DEV_OUT.info("Creating split module " + splitId + " after approval from the incremental manager");
                splitCurrent = SplitModuleUtil.createSplitModule(getOwner(), true);
            } else {
                TDebug.DEV_OUT.info("NOT creating split module " + splitId + " after disapproval from the incremental manager");
            }
        } else {
            //c. The split module does not exist: create it
            if (Context.context().getIncrementalManager().createSplitModuleRequest(ownerId)) {
                TDebug.DEV_OUT.info("Creating split module " + splitId + " after approval from the incremental manager");
                splitCurrent = SplitModuleUtil.createSplitModule(getOwner(), true);
            } else {
                TDebug.DEV_OUT.info("NOT creating split module " + splitId + " after disapproval from the incremental manager");
            }
        }
    }
    
    /**
     * Called to finish the solving.
     * 
     * @return
     *      the solver result
     */
    public MSolverResult finishSolver() {
        // invariant: there should be no remaining active constraints
        if(constraints.activeSize() > 0) {
            debug.warn("Expected no remaining active constraints, but got ", constraints.activeSize());
        }

        final Map<IConstraint, Delay> delayed = constraints.delayed();
        debug.info("Delayed log:");
        delayedLog.flush(debug.subContext());
        debug.info("Solved {} constraints ({} delays) with {} failed, and {} remaining constraint(s).",
                reductions, delays, failed.size(), constraints.delayedSize());
        logTimes("success", successCount, debug);
        logTimes("delay", delayCount, debug);

        final Map<ITermVar, ITermVar> existentials = Optional.ofNullable(this.existentials).orElse(ImmutableMap.of());
        MSolverResult result = MSolverResult.of(state, failed, delayed, existentials);
        
        Context.context().getIncrementalManager().solverDone(this, result);
        init = false;
        return result;
    }

    private static void addTime(IConstraint c, long dt, Map<Class<? extends IConstraint>, Long> times,
            IDebugContext debug) {
        if(!debug.isEnabled(Level.Info)) {
            return;
        }
        final Class<? extends IConstraint> key = c.getClass();
        final long t = times.getOrDefault(key, 0L).longValue() + dt;
        times.put(key, t);
    }

    private static void logTimes(String name, Map<Class<? extends IConstraint>, Long> times, IDebugContext debug) {
        debug.info("# ----- {} -----", name);
        for(Map.Entry<Class<? extends IConstraint>, Long> entry : times.entrySet()) {
            debug.info("{} : {}x", entry.getKey().getSimpleName(), entry.getValue());
        }
        debug.info("# ----- {} -----", "-");
    }

    public static Optional<MSolverResult> entails(final IMState state, final IConstraint constraint,
            final IsComplete isComplete, final IDebugContext debug)
            throws InterruptedException, Delay {
        debug.debug("Entails for {}", state.owner().getId());
        if(debug.isEnabled(Level.Info)) {
            debug.info("Checking entailment of {}", toString(constraint, state.unifier()));
        }
        
        // remove all previously created variables/scopes to make them rigid/closed
        final IMState _state = state.delegate(Collections.emptySet(), true);
        final MSolverResult result = solveSeparately(_state, constraint, isComplete, debug);
        
        debug.trace("Completed entails");
        
        if(result.hasErrors()) {
            debug.info("Constraints not entailed");
            return Optional.empty();
        } else if(result.delays().isEmpty()) {
            debug.info("Constraints entailed");
            return Optional.of(result);
        } else {
            debug.info("Cannot decide constraint entailment (unsolved constraints)");
            // FIXME this doesn't remove rigid vars, as they are not part of State.vars()
            throw result.delay().removeAll(_state.vars(), _state.scopes());
        }
    }

    private static void printTrace(IConstraint failed, IUnifier.Immutable unifier, IDebugContext debug) {
        if (!debug.isEnabled(Level.Error)) return;
        @Nullable IConstraint constraint = failed;
        while(constraint != null) {
            debug.error(" * {}", constraint.toString(ModuleSolver.shallowTermFormatter(unifier)));
            constraint = constraint.cause().orElse(null);
        }
    }

    static String toString(IConstraint constraint, IUnifier.Immutable unifier) {
        return constraint.toString(Solver.shallowTermFormatter(unifier));
    }
    
    static String toString(Iterable<IConstraint> constraints, IUnifier.Immutable unifier) {
        final StringBuilder sb = new StringBuilder();
        boolean first = true;
        for(IConstraint constraint : constraints) {
            if(first) {
                first = false;
            } else {
                sb.append(", ");
            }
            sb.append(constraint.toString(ModuleSolver.shallowTermFormatter(unifier)));
        }
        return sb.toString();
    }

    public static TermFormatter shallowTermFormatter(final IUnifier unifier) {
        return new UnifierFormatter(unifier, 3);
    }
    
    @Override
    public String toString() {
        return "ModuleSolver<" + getOwner().getId() + ">";
    }
}