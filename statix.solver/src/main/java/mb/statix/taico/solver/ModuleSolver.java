package mb.statix.taico.solver;

import static mb.statix.taico.util.TDebug.*;

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

import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.ITermVar;
import mb.nabl2.terms.unification.IUnifier;
import mb.nabl2.terms.unification.UnifierFormatter;
import mb.nabl2.util.TermFormatter;
import mb.statix.scopegraph.terms.Scope;
import mb.statix.solver.Delay;
import mb.statix.solver.IConstraint;
import mb.statix.solver.completeness.Completeness;
import mb.statix.solver.completeness.IsComplete;
import mb.statix.solver.log.IDebugContext;
import mb.statix.solver.log.LazyDebugContext;
import mb.statix.solver.log.Log;
import mb.statix.solver.log.PrefixedDebugContext;
import mb.statix.solver.persistent.Solver;
import mb.statix.taico.module.IModule;
import mb.statix.taico.solver.completeness.RedirectingIncrementalCompleteness;
import mb.statix.taico.solver.concurrent.ConcurrentRedirectingIncrementalCompleteness;
import mb.statix.taico.solver.store.ModuleConstraintStore;
import mb.statix.taico.util.IOwnable;
import mb.statix.taico.util.TOverrides;

public class ModuleSolver implements IOwnable {
    private final IMState state;
    private Map<ITermVar, ITermVar> existentials = null;
    private final ModuleConstraintStore constraints;
    private final RedirectingIncrementalCompleteness completeness;
    private final PrefixedDebugContext debug;
    private final LazyDebugContext proxyDebug;
    private boolean separateSolver;
    private boolean init;
    
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
        return new ModuleSolver(state, constraint, (s, l, st) -> true, topDebug, false);
    }
    
    private ModuleSolver(IMState state, IConstraint constraint, IsComplete _isComplete, PrefixedDebugContext debug, boolean separateSolver) {
        final String owner = state.owner().getId();
        this.state = state;
        this.debug = debug;
        this.proxyDebug = new LazyDebugContext(debug);
        this.separateSolver = separateSolver;
        
        //Create the constraint store and the completeness. Also add the initial constraints
        Iterable<IConstraint> constraintList = constraint == null ? Collections.emptyList() : Collections.singletonList(constraint);
        this.constraints = new ModuleConstraintStore(owner, constraintList, debug);
        this.completeness = TOverrides.CONCURRENT ? new ConcurrentRedirectingIncrementalCompleteness(owner, state.spec()) : new RedirectingIncrementalCompleteness(owner, state.spec());
        this.completeness.addAll(constraintList, state.unifier());
        
        if (_isComplete == null) {
            this.isComplete = (s, l, u) -> completeness.isComplete(s, l, state.unifier());
        } else {
            this.isComplete = (s, l, u) -> completeness.isComplete(s, l, state.unifier()) && _isComplete.test(s, l, u);
        }

        state.setSolver(this);
        if (!separateSolver) SolverContext.context().getIncrementalManager().initSolver(this);
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
        PrefixedDebugContext debug = this.debug.createSibling(state.owner().getId());
        ModuleSolver solver = new ModuleSolver(state, constraint, this.isComplete, debug, false);
        
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
        // TODO Should report no constriants to solve, is this still true with CTrue?
        ModuleSolver solver = new ModuleSolver(state, null, this.isComplete, debug, false);
        return solver;
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
     *      true if another step is required, false otherwise
     * @throws InterruptedException
     */
    public boolean solveStep() throws InterruptedException {
        if (!init) {
            SolverContext.context().getIncrementalManager().solverStart(this);
            init = true;
        }
        IConstraint constraint = constraints.remove();
        if (constraint == null) return false;
    
        IDebugContext subDebug = CONSTRAINT_SOLVING ? proxyDebug.subContext() : DEV_NULL;
        if(proxyDebug.isEnabled(Level.Info)) {
            proxyDebug.info("Solving {}", constraint.toString(ModuleSolver.shallowTermFormatter(state.unifier())));
        }
        
        try {
            final Optional<MConstraintResult> maybeResult;
            maybeResult =
                    constraint.solve(state, new MConstraintContext(isComplete, subDebug));
            addTime(constraint, 1, successCount, debug);
            
            reductions += 1;
            if(maybeResult.isPresent()) {
                final MConstraintResult result = maybeResult.get();
                if(existentials == null) {
                    existentials = result.existentials();
                }
                if(!result.constraints().isEmpty()) {
                    final List<IConstraint> newConstaints = result.constraints().stream()
                            .map(c -> c.withCause(constraint)).collect(Collectors.toList());
                    if(subDebug.isEnabled(Level.Info)) {
                        subDebug.info("Simplified to {}", toString(newConstaints, state.unifier()));
                    }
                    constraints.addAll(newConstaints);
                    completeness.addAll(newConstaints, state.unifier());
                }
                //Only remove the solved constraint after new constraints are added (for concurrent consistency)
                completeness.remove(constraint, state.unifier());
                
                //Activate constraints after updating the completeness
                completeness.updateAll(result.vars(), state.unifier());
                constraints.activateFromVars(result.vars(), subDebug);
                
                if (!TOverrides.USE_OBSERVER_MECHANISM_FOR_SELF) constraints.activateFromEdges(Completeness.criticalEdges(constraint, state.spec(), state.unifier()), subDebug);
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
            constraints.delay(constraint, d);
            delays += 1;
        } catch (Exception ex) {
            System.err.println("FATAL: Exception encountered while solving!");
            ex.printStackTrace();
            return false;
        }
        return true;
    }
    
    /**
     * @param scope
     *      the scope of the edge
     * @param label
     *      the label of the edge
     * 
     * @return
     *      true if the given edge is complete for this module (no children are checked), false
     *      otherwise
     */
    public boolean isCompleteSelf(Scope scope, ITerm label) {
        return completeness.isComplete(scope, label, state.unifier()) && isComplete.test(scope, label, state);
    }
    
    public void computeCompleteness(Set<String> incompleteModules, Scope scope, ITerm label) {
        if (COMPLETENESS) System.err.println("Completeness of " + getOwner() + " got query for " + scope + "-" + label + ".");
        
        //We, or one of our parents will be the scope owner
        
        //Check if we ourselves are incomplete. If we are, we cannot be sure that all our children have been created, so we will stop there.
        if (!isCompleteSelf(scope, label)) {
            incompleteModules.add(getOwner().getId());
            return;
        }
        
        //TODO Not all our children might have been created yet, but we must ensure that this list is complete
        //Check our children
        for (IModule child : getOwner().getChildren()) {
            //Only delegate to children who get passed the scope
            if (!child.getScopeGraph().getExtensibleScopes().contains(scope)) continue;
            
            child.getCurrentState().solver().computeCompleteness(incompleteModules, scope, label);
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
        
        if (getOwner().getCurrentState().solver() == this) {
            //If we are the main solver of this module, we signal that we are done.
            SolverContext.context().getIncrementalManager().solverDone(this);
            init = false;
        }

        final Map<IConstraint, Delay> delayed = constraints.delayed();
        debug.info("Delayed log:");
        delayedLog.flush(debug.subContext());
        debug.info("Solved {} constraints ({} delays) with {} failed, and {} remaining constraint(s).",
                reductions, delays, failed.size(), constraints.delayedSize());
        logTimes("success", successCount, debug);
        logTimes("delay", delayCount, debug);

        final Map<ITermVar, ITermVar> existentials = Optional.ofNullable(this.existentials).orElse(ImmutableMap.of());
        return MSolverResult.of(state, failed, delayed, existentials);
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

    public static TermFormatter shallowTermFormatter(final IUnifier.Immutable unifier) {
        return new UnifierFormatter(unifier, 3);
    }
    
    @Override
    public String toString() {
        return "ModuleSolver<" + getOwner().getId() + ">";
    }
}