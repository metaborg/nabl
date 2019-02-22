package mb.statix.taico.solver;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import org.metaborg.util.functions.Predicate1;
import org.metaborg.util.log.Level;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;

import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.ITermVar;
import mb.nabl2.terms.unification.IUnifier;
import mb.nabl2.terms.unification.UnifierFormatter;
import mb.nabl2.util.TermFormatter;
import mb.statix.concurrent.solver.ConcurrentSolver;
import mb.statix.solver.Completeness;
import mb.statix.solver.ConstraintContext;
import mb.statix.solver.Delay;
import mb.statix.solver.IConstraint;
import mb.statix.solver.IConstraintStore.Entry;
import mb.statix.solver.Solver;
import mb.statix.solver.SolverResult;
import mb.statix.solver.State;
import mb.statix.solver.log.IDebugContext;
import mb.statix.solver.log.LazyDebugContext;
import mb.statix.solver.log.Log;
import mb.statix.taico.module.IModule;
import mb.statix.taico.solver.store.ModuleConstraintStore;
import mb.statix.taico.util.IOwnable;

public class ModuleSolver implements IOwnable {
    private ModuleSolver parent;
    private MState state;
    private ModuleConstraintStore constraints;
    private MCompleteness completeness;
    private IDebugContext debug;
    private final LazyDebugContext proxyDebug;
    
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
        return new ModuleSolver(null, state, constraints, new MCompleteness(), v -> false, s -> false, debug);
    }
    
    private ModuleSolver(ModuleSolver parent, MState state, Iterable<IConstraint> constraints, MCompleteness completeness, Predicate1<ITermVar> isRigid, Predicate1<ITerm> isClosed, IDebugContext debug) {
        this.parent = parent;
        this.state = state;
        this.constraints = new ModuleConstraintStore(constraints, debug);
        this.completeness = completeness;
        this.completeness.addAll(constraints);
        this.isRigid = isRigid;
        this.isClosed = isClosed;
        this.debug = debug;
        this.proxyDebug = new LazyDebugContext(debug);
    }
    
    /**
     * Creates a solver as a child of this solver.
     * 
     * @param state
     *      the newly created state for this solver
     * @param constraints
     * @param isRigid
     * @param isClosed
     * @param debug
     * @return
     */
    public ModuleSolver childSolver(MState state, Iterable<IConstraint> constraints, Predicate1<ITermVar> isRigid, Predicate1<ITerm> isClosed, IDebugContext debug) {
        ModuleSolver solver = new ModuleSolver(this, state, constraints, new MCompleteness(), isRigid, isClosed, debug);
        
        this.state.coordinator().addSolver(solver);
        
        return solver;
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
    
    /**
     * Reports if any progress has been made since the last check.
     * 
     * @return
     *      true if progress has been made, false otherwise
     */
    protected boolean checkAndResetProgress() {
        return constraints.checkProgressAndReset();
    }
    
    //TODO TAICO Are critical edges determined from the constraints that are left?
    
    //TODO TAICO Orchestrate the solvers in such a way that 
    
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
            proxyDebug.info("Solving {}", constraint.toString(ConcurrentSolver.shallowTermFormatter(state.unifier())));
        }
        try {
            final Optional<MConstraintResult> maybeResult;
            maybeResult =
                    constraint.solveMutable(state, new ConstraintContext(completeness.freeze(), isRigid, isClosed, subDebug));
            addTime(constraint, 1, successCount, debug);
            entry.remove();
            completeness.remove(constraint);
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
                constraints.activateFromVars(result.vars(), subDebug);
                //TODO TAICO CRITICALEDGES Fix critical edge mechanism
                //constraints.activateFromEdges(Completeness.criticalEdges(constraint, result.state()), subDebug);
            } else {
                subDebug.error("Failed");
                failed.add(constraint);
                if(proxyDebug.isRoot()) {
                    printTrace(constraint, state.unifier(), subDebug);
                } else {
                    proxyDebug.info("Break early because of errors.");
                    return false;
                }
            }
            proxyDebug.commit();
        } catch(Delay d) {
            addTime(constraint, 1, delayCount, debug);
            subDebug.info("Delayed");
            delayedLog.absorb(proxyDebug.clear());
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
    public SolverResult finishSolver() throws InterruptedException {
        // invariant: there should be no remaining active constraints
        if(constraints.activeSize() > 0) {
            debug.warn("Expected no remaining active constraints, but got ", constraints.activeSize());
        }

        final Map<IConstraint, Delay> delayed = constraints.delayed();
        delayedLog.flush(debug);
        debug.info("[{}] Solved {} constraints ({} delays) with {} failed, and {} remaining constraint(s).",
                getOwner().getId(), reductions, delays, failed.size(), constraints.delayedSize());
        logTimes("success", successCount, debug);
        logTimes("delay", delayCount, debug);

        return SolverResult.of(null, completeness, failed, delayed);
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

    public Optional<SolverResult> entails(final State state, final Iterable<IConstraint> constraints,
            final Completeness completeness, final IDebugContext debug) throws InterruptedException, Delay {
        return entails(state, constraints, completeness, ImmutableSet.of(), debug);
    }

    public Optional<SolverResult> entails(final State state, final Iterable<IConstraint> constraints,
            final Completeness completeness, final Iterable<ITermVar> _localVars, final IDebugContext debug)
            throws InterruptedException, Delay {
        if(debug.isEnabled(Level.Info)) {
            debug.info("Checking entailment of {}", toString(constraints, state.unifier()));
        }
        final Set<ITermVar> localVars = ImmutableSet.copyOf(_localVars);
        final Set<ITermVar> rigidVars = Sets.difference(state.vars(), localVars);
        final SolverResult result = Solver.solve(state, constraints, completeness, rigidVars::contains,
                state.scopes()::contains, debug.subContext());
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

    public static TermFormatter shallowTermFormatter(final IUnifier.Immutable unifier) {
        return new UnifierFormatter(unifier, 3);
    }
}