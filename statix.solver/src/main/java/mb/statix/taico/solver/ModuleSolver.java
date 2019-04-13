package mb.statix.taico.solver;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import org.immutables.value.Value;
import org.metaborg.util.functions.Predicate1;
import org.metaborg.util.log.Level;

import com.google.common.collect.ImmutableSet;

import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.ITermVar;
import mb.nabl2.terms.unification.IUnifier;
import mb.nabl2.terms.unification.UnifierFormatter;
import mb.nabl2.util.TermFormatter;
import mb.statix.solver.Delay;
import mb.statix.solver.IConstraint;
import mb.statix.solver.IConstraintStore.Entry;
import mb.statix.solver.ISolverResult;
import mb.statix.solver.log.IDebugContext;
import mb.statix.solver.log.LazyDebugContext;
import mb.statix.solver.log.Log;
import mb.statix.solver.log.PrefixedDebugContext;
import mb.statix.taico.module.IModule;
import mb.statix.taico.scopegraph.OwnableScope;
import mb.statix.taico.solver.store.ModuleConstraintStore;
import mb.statix.taico.util.IOwnable;

public class ModuleSolver implements IOwnable {
    private ModuleSolver parent;
    private MState state;
    private ModuleConstraintStore constraints;
    private MCompleteness completeness;
    private PrefixedDebugContext debug;
    private final LazyDebugContext proxyDebug;
    private boolean separateSolver;
    
    private Predicate1<ITermVar> isRigid;
    private Predicate1<ITerm> isClosed;
    
    // time log
    private final Map<Class<? extends IConstraint>, Long> successCount = new HashMap<>();
    private final Map<Class<? extends IConstraint>, Long> delayCount = new HashMap<>();

    // fixed point
    private final Set<IConstraint> failed = new HashSet<>();
    private final Log delayedLog = new Log();
    private int reductions = 0;
    private int delays = 0;
    
    public static ModuleSolver topLevelSolver(MState state, Iterable<IConstraint> constraints, IDebugContext debug) {
        PrefixedDebugContext topDebug = new PrefixedDebugContext(state.owner().getId(), debug);
        MCompleteness completeness = MCompleteness.topLevelCompleteness(state.owner());
        return new ModuleSolver(null, state, constraints, completeness, v -> false, s -> false, topDebug);
    }
    
    private ModuleSolver(ModuleSolver parent, MState state, Iterable<IConstraint> constraints, MCompleteness completeness, Predicate1<ITermVar> isRigid, Predicate1<ITerm> isClosed, PrefixedDebugContext debug) {
        this.parent = parent;
        this.state = state;
        this.constraints = new ModuleConstraintStore(state.manager(), constraints, debug);
        this.completeness = completeness;
        this.completeness.addAll(constraints);
        this.isRigid = isRigid;
        this.isClosed = isClosed;
        this.debug = debug;
        this.proxyDebug = new LazyDebugContext(debug);
        
        state.setSolver(this);
    }
    
    /**
     * Creates a solver as a child of this solver.
     * 
     * @param state
     *      the newly created state for this solver
     * @param constraints
     *      the constraints to solve
     * @param isRigid
     *      predicate to determine if term variables are rigid
     * @param isClosed
     *      predicate to determine of scopes are closed
     * @return
     *      the new solver
     */
    public ModuleSolver childSolver(MState state, Iterable<IConstraint> constraints, Predicate1<ITermVar> isRigid, Predicate1<ITerm> isClosed) {
        PrefixedDebugContext debug = this.debug.createSibling(state.owner().getId());
        MCompleteness childCompleteness = completeness.createChild(state.owner());
        ModuleSolver solver = new ModuleSolver(this, state, constraints, childCompleteness, isRigid, isClosed, debug);
        
        this.state.coordinator().addSolver(solver);
        
        return solver;
    }
    
    /**
     * Solves the given arguments separately from other solvers, in an isolated context.
     * 
     * @param state
     * @param constraints
     * @param completeness
     * @param isRigid
     * @param isClosed
     * @param debug
     * @return
     * @throws InterruptedException
     */
    public static MSolverResult solveSeparately(
            MState state,
            Iterable<IConstraint> constraints,
            MCompleteness completeness,
            Predicate1<ITermVar> isRigid,
            Predicate1<ITerm> isClosed,
            IDebugContext debug) throws InterruptedException {
        PrefixedDebugContext debug2 = new PrefixedDebugContext("", debug.subContext());
        //TODO Can this cross module boundaries?
        ModuleSolver solver = new ModuleSolver(null, state.copy(), constraints, completeness.copy(), isRigid, isClosed, debug2);
        solver.separateSolver = true;
        while (solver.solveStep());
        return solver.finishSolver();
    }
    
    @Override
    public IModule getOwner() {
        return state.owner();
    }
    
    /**
     * @return
     *      the parent solver of this solver, or null if this is the top level solver
     */
    public ModuleSolver getParent() {
        return parent;
    }
    
    public Predicate1<ITermVar> isRigid() {
        return isRigid;
    }
    
    public Predicate1<ITerm> isClosed() {
        return isClosed;
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
     * Reports if any progress has been made since the last check.
     * 
     * @return
     *      true if progress has been made, false otherwise
     */
    protected boolean checkAndResetProgress() {
        return constraints.checkProgressAndReset();
    }
    
    /**
     * The solver is guaranteed to be done if it has no more constraints.
     * It should be able to be done even if there are child solvers still solving.
     * 
     * @return
     *      true if this solver is done, false otherwise
     */
    public boolean isDone() {
        return constraints.isDone();
    }
    
    /**
     * @return
     *      true if this solver is stuck waiting, false otherwise
     */
    public boolean isStuck() {
        return constraints.isStuck();
    }
    
    /**
     * This method notifies this solver of progress that has been made by other solvers.
     */
    public void externalProgress() {
        constraints.externalProgress();
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
            final Optional<MConstraintResult> maybeResult;
            maybeResult =
                    constraint.solve(state, new MConstraintContext(completeness, isRigid, isClosed, subDebug));
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
                //Only remove the solved constraint once new constraints are added
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
        }
        return true;
    }
    
    /**
     * Called to finish the solving.
     * 
     * @return
     * @throws InterruptedException
     */
    public MSolverResult finishSolver() throws InterruptedException {
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

        return MSolverResult.of(state, completeness, failed, delayed);
    }

    private void addTime(IConstraint c, long dt, Map<Class<? extends IConstraint>, Long> times,
            IDebugContext debug) {
        if(!debug.isEnabled(Level.Info)) {
            return;
        }
        final Class<? extends IConstraint> key = c.getClass();
        final long t = times.getOrDefault(key, 0L).longValue() + dt;
        times.put(key, t);
    }

    private void logTimes(String name, Map<Class<? extends IConstraint>, Long> times, IDebugContext debug) {
        debug.info("# ----- {} -----", name);
        for(Map.Entry<Class<? extends IConstraint>, Long> entry : times.entrySet()) {
            debug.info("{} : {}x", entry.getKey().getSimpleName(), entry.getValue());
        }
        debug.info("# ----- {} -----", "-");
    }

    public static Optional<MSolverResult> entails(final MState state, final Iterable<IConstraint> constraints,
            final MCompleteness completeness, final IDebugContext debug) throws InterruptedException, Delay {
        return entails(state, constraints, completeness, ImmutableSet.of(), debug);
    }

    public static Optional<MSolverResult> entails(final MState state, final Iterable<IConstraint> constraints,
            final MCompleteness completeness, final Iterable<ITermVar> _localVars, final IDebugContext debug)
            throws InterruptedException, Delay {
        debug.debug("Entails for {}", state.owner().getId());
        if(debug.isEnabled(Level.Info)) {
            debug.info("Checking entailment of {}", toString(constraints, state.unifier()));
        }
        
        //Rigid vars = all variables in the state that are not in the local variables + all variables owned by other modules
        final Set<ITermVar> localVars = ImmutableSet.copyOf(_localVars);
        final Set<ITermVar> rigidVars = new HashSet<>(state.vars());
        rigidVars.removeAll(localVars);
        PrefixedDebugContext debug2 = new PrefixedDebugContext("", debug.subContext());
        
        final Predicate1<ITermVar> isRigid = v -> {
            //if owner == state.owner, return false.
            if (rigidVars.contains(v)) return true;
            
            IModule owner = state.manager().getModule(v.getResource());
            System.err.println("[1] isRigid matcher with new owner " + owner);
            return owner != state.owner();
        };
        
        final Predicate1<ITerm> isClosed = s -> {
            if (state.scopeGraph().getScopes().contains(s)) return true;
            
            IModule owner;
            if (s instanceof IOwnable) {
                System.err.println("isClosed in MConstraintLabelWF (accepting), s is ownable");
                owner = ((IOwnable) s).getOwner();
            } else {
                System.err.println("isClosed in MConstraintLabelWF (accepting), s is NOT ownable");
                OwnableScope scope = OwnableScope.ownableMatcher(state.manager()::getModule).match(s, state.unifier()).orElse(null);
                if (scope == null) {
                    System.err.println("Unable to convert scope term to scope in isClosed predicate in MConstraintLabelWF (accepting)");
                    return false;
                }
                owner = scope.getOwner();
            }
            return owner != state.owner();
        };
        //TODO IMPORTANT TAICO Is this correct?
        //TODO TAICO can this cross module boundaries?
        ModuleSolver solver = new ModuleSolver(null, state, constraints, completeness, isRigid, isClosed, debug2);
        solver.separateSolver = true;
        while (solver.solveStep());
        final MSolverResult result = solver.finishSolver();
        
        debug.trace("Completed entails");
        
        if(result.hasErrors()) {
            debug.info("Constraints not entailed");
            return Optional.empty();
        } else if(result.delays().isEmpty()) {
            debug.info("Constraints entailed");
            return Optional.of(result);
        } else {
            debug.info("Cannot decide constraint entailment (unsolved constraints)");
            throw result.delay().retainAll(state.vars(), state.scopes());
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
    public static abstract class AMSolverResult implements ISolverResult {

        @Value.Parameter public abstract MState state();

        @Value.Parameter public abstract MCompleteness completeness();

        @Value.Parameter public abstract Set<IConstraint> errors();

        @Value.Parameter public abstract Map<IConstraint, Delay> delays();

    }

    public static TermFormatter shallowTermFormatter(final IUnifier.Immutable unifier) {
        return new UnifierFormatter(unifier, 3);
    }
    
    @Override
    public String toString() {
        return "ModuleSolver<" + getOwner().getId() + ">";
    }
}