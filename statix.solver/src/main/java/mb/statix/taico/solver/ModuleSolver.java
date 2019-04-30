package mb.statix.taico.solver;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.checkerframework.checker.nullness.qual.Nullable;

import org.immutables.serial.Serial;
import org.immutables.value.Value;
import org.metaborg.util.log.Level;

import com.google.common.collect.ImmutableSet;

import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.ITermVar;
import mb.nabl2.terms.unification.IUnifier;
import mb.nabl2.terms.unification.UnifierFormatter;
import mb.nabl2.terms.unification.IUnifier.Immutable;
import mb.nabl2.util.CapsuleUtil;
import mb.nabl2.util.TermFormatter;
import mb.statix.scopegraph.terms.Scope;
import mb.statix.solver.Delay;
import mb.statix.solver.IConstraint;
import mb.statix.solver.IConstraintStore.Entry;
import mb.statix.solver.ISolverResult;
import mb.statix.solver.log.IDebugContext;
import mb.statix.solver.log.LazyDebugContext;
import mb.statix.solver.log.Log;
import mb.statix.solver.log.PrefixedDebugContext;
import mb.statix.taico.module.IModule;
import mb.statix.taico.module.ModuleCleanliness;
import mb.statix.taico.solver.store.ModuleConstraintStore;
import mb.statix.taico.util.IOwnable;
import mb.statix.taico.util.Scopes;

public class ModuleSolver implements IOwnable {
    private final IMState state;
    private final ModuleConstraintStore constraints;
    private final MCompleteness completeness;
    private final PrefixedDebugContext debug;
    private final LazyDebugContext proxyDebug;
    private boolean separateSolver;
    
    private ICompleteness isComplete;
    
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
    public static ModuleSolver topLevelSolver(IMState state, Iterable<IConstraint> constraints, IDebugContext debug) {
        PrefixedDebugContext topDebug = new PrefixedDebugContext(state.owner().getId(), debug);
        return new ModuleSolver(state, constraints, (s, l, st) -> CompletenessResult.of(true, null), topDebug);
    }
    
    private ModuleSolver(IMState state, Iterable<IConstraint> constraints, ICompleteness isComplete, PrefixedDebugContext debug) {
        this.state = state;
        this.constraints = new ModuleConstraintStore(constraints, debug);
        this.completeness = new MCompleteness(state.owner(), constraints);
        this.isComplete = isComplete;
        this.debug = debug;
        this.proxyDebug = new LazyDebugContext(debug);
        
        state.setSolver(this);
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
    public ModuleSolver childSolver(IMState state, Iterable<IConstraint> constraints) {
        PrefixedDebugContext debug = this.debug.createSibling(state.owner().getId());
        ModuleSolver solver = new ModuleSolver(state, constraints, this.isComplete, debug);
        
        this.state.coordinator().addSolver(solver);
        
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
            Iterable<IConstraint> constraints,
            ICompleteness isComplete,
            IDebugContext debug) throws InterruptedException {
        PrefixedDebugContext debug2 = new PrefixedDebugContext("", debug.subContext());
        ModuleSolver solver = new ModuleSolver(state, constraints, isComplete, debug2);
        solver.separateSolver = true;
        while (solver.solveStep());
        return solver.finishSolver();
    }
    
    @Override
    public IModule getOwner() {
        return state.owner();
    }
    
    public MCompleteness getCompleteness() {
        return completeness;
    }
    
    public ModuleConstraintStore getStore() {
        return constraints;
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
        Entry entry = constraints.getActiveConstraint(debug);
        if (entry == null) return false;
    
        IDebugContext subDebug = proxyDebug.subContext();
        final IConstraint constraint = entry.constraint();
        if(proxyDebug.isEnabled(Level.Info)) {
            proxyDebug.info("Solving {}", constraint.toString(ModuleSolver.shallowTermFormatter(state.unifier())));
        }
        
        try {
            final ICompleteness isComplete = this::isComplete;
            final Optional<MConstraintResult> maybeResult;
            maybeResult =
                    constraint.solve(state, new MConstraintContext(isComplete, subDebug));
            addTime(constraint, 1, successCount, debug);
            entry.remove();
            
            reductions += 1;
            if(maybeResult.isPresent()) {
                final MConstraintResult result = maybeResult.get();
                if(!result.constraints().isEmpty()) {
                    final List<IConstraint> newConstaints = result.constraints().stream()
                            .map(c -> c.withCause(constraint)).collect(Collectors.toList());
                    if(subDebug.isEnabled(Level.Info)) {
                        subDebug.info("Simplified to {}", toString(newConstaints, state.unifier()));
                    }
                    constraints.addAll(newConstaints);
                    completeness.addAll(newConstaints);
                }
                //Only remove the solved constraint after new constraints are added (for concurrent consistency)
                completeness.remove(constraint);
                
                //Activate constraints after updating the completeness
                constraints.activateFromVars(result.vars(), subDebug);
                constraints.activateFromEdges(MCompleteness.criticalEdges(constraint, state), subDebug);
            } else {
                completeness.remove(constraint);
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
            entry.delay(d);
            delays += 1;
        } catch (Exception ex) {
            System.err.println("FATAL: Exception encountered while solving!");
            ex.printStackTrace();
        }
        return true;
    }
    
    /**
     * Checks if the given scope and label are complete. The result is delegated to children where
     * necessary.
     * 
     * @param scope
     *      the scope
     * @param label
     *      the label
     * @param state
     *      the state
     * 
     * @return
     *      the completeness result
     */
    public CompletenessResult isComplete(ITerm scopeTerm, ITerm label, IMState state) {
        System.err.println(getOwner() + " received isComplete query for " + scopeTerm);
        if (state.getOwner() != getOwner()) debug.warn("Received isComplete query on {} for state of {}", getOwner(), state.getOwner());

        Scope scope = Scopes.getScope(scopeTerm);
        
        IModule scopeOwner;
        try {
            scopeOwner = state.context().getModule(state.getOwner(), scope.getResource());
        } catch (Delay d) {
            return CompletenessResult.of(false, getOwner()).withDelay(d);
        }
        if (scopeOwner == null) throw new IllegalStateException("Encountered scope without owning module: " + scope);
        
        CompletenessResult result;
        if (scopeOwner != getOwner()) {
            result = scopeOwner.getCurrentState().solver().isCompleteFinal(scope, label, state);
        } else {
            result = isCompleteFinal(scope, label, state);
        }
        if (!result.isComplete()) return result;

        return isComplete.apply(scopeTerm, label, state);
    }
    
    private CompletenessResult isCompleteFinal(Scope scope, ITerm label, IMState state) {
        System.err.println("Completeness of " + getOwner() + " got isCompleteFinal request.");
        CompletenessResult r = completeness.isComplete(scope, label, state);
        if (!r.isComplete()) {
            System.err.println("Completeness of " + getOwner() + " result: (own completeness) false");
            return r;
        }
        
        //TODO Unchecked access to children. Should go via context. Requester is in the state
        for (IModule child : getOwner().getChildren()) {
            //TODO OPTIMIZATION Only delegate to children who get passed the scope
            //if (!child.getScopeGraph().getExtensibleScopes().contains(scope)) continue;
            
            CompletenessResult childResult = child.getCurrentState().solver().isCompleteFinal(scope, label, state);
            if (!childResult.isComplete()) {
                System.err.println("Completeness of " + getOwner() + " result: (child) false");
                return childResult;
            }
        }
        
        r = isComplete.apply(scope, label, state);
        System.err.println("Completeness of " + getOwner() + " result: (isComplete predicate) " + r.isComplete());
        return r;
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
            //Flag the module as clean if we are its main solver.
            getOwner().flag(ModuleCleanliness.CLEAN);
        }

        final Map<IConstraint, Delay> delayed = constraints.delayed();
        debug.info("Delayed log:");
        delayedLog.flush(debug.subContext());
        debug.info("Solved {} constraints ({} delays) with {} failed, and {} remaining constraint(s).",
                reductions, delays, failed.size(), constraints.delayedSize());
        logTimes("success", successCount, debug);
        logTimes("delay", delayCount, debug);

        return MSolverResult.of(state, failed, delayed);
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

    public static Optional<MSolverResult> entails(IMState state, Iterable<IConstraint> constraints,
            ICompleteness isComplete, IDebugContext debug)
            throws InterruptedException, Delay {
        return entails(state, constraints, isComplete, ImmutableSet.of(), debug);
    }

    public static Optional<MSolverResult> entails(final IMState state, final Iterable<IConstraint> constraints,
            final ICompleteness isComplete, final Iterable<ITermVar> _localVars, final IDebugContext debug)
            throws InterruptedException, Delay {
        debug.debug("Entails for {}", state.owner().getId());
        if(debug.isEnabled(Level.Info)) {
            debug.info("Checking entailment of {}", toString(constraints, state.unifier()));
        }
        
        // remove all previously created variables/scopes to make them rigid/closed
        final IMState _state = state.delegate(CapsuleUtil.toSet(_localVars), true);
        final MSolverResult result = solveSeparately(_state, constraints, isComplete, debug);
        
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
        @Nullable IConstraint constraint = failed;
        while(constraint != null) {
            debug.error(" * {}", constraint.toString(ModuleSolver.shallowTermFormatter(unifier)));
            constraint = constraint.cause().orElse(null);
        }
    }

    private static String toString(Iterable<IConstraint> constraints, IUnifier.Immutable unifier) {
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
    
    @Value.Immutable
    @Serial.Version(42L)
    public static abstract class AMSolverResult implements ISolverResult {

        @Value.Parameter public abstract IMState state();

        @Value.Parameter public abstract Set<IConstraint> errors();

        @Value.Parameter public abstract Map<IConstraint, Delay> delays();

        @Override
        public Immutable unifier() {
            return state().unifier();
        }
        
        /**
         * Resets all errors and delays on this solver result.
         * 
         * @return
         *      a new solver result
         */
        public MSolverResult reset() {
            return MSolverResult.of(state(), new HashSet<>(), new HashMap<>());
        }
    }

    public static TermFormatter shallowTermFormatter(final IUnifier.Immutable unifier) {
        return new UnifierFormatter(unifier, 3);
    }
    
    @Override
    public String toString() {
        return "ModuleSolver<" + getOwner().getId() + ">";
    }
}