package mb.statix.concurrent.solver;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Callable;
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
import mb.statix.concurrent.solver.store.ConcurrentConstraintStore;
import mb.statix.concurrent.solver.store.ConcurrentConstraintStore.ConcurrentEntry;
import mb.statix.solver.Completeness;
import mb.statix.solver.ConstraintContext;
import mb.statix.solver.ConstraintResult;
import mb.statix.solver.Delay;
import mb.statix.solver.IConstraint;
import mb.statix.solver.Solver;
import mb.statix.solver.SolverResult;
import mb.statix.solver.State;
import mb.statix.solver.log.IDebugContext;
import mb.statix.solver.log.LazyDebugContext;
import mb.statix.solver.log.Log;

public class ConcurrentSolver implements Callable<SolverResult> {
    private Iterable<IConstraint> initConstraints;
    private Completeness initCompleteness;
    private IDebugContext debug;
    private Predicate1<ITermVar> isRigid;
    private Predicate1<ITerm> isClosed;
    
    private final LazyDebugContext proxyDebug;

    // set-up
    private final ConcurrentConstraintStore constraints;
    private State state;
    private ConcurrentCompleteness completeness;

    // time log
    private final Map<Class<? extends IConstraint>, Long> successCount = new HashMap<>();
    private final Map<Class<? extends IConstraint>, Long> delayCount = new HashMap<>();

    // fixed point
    private final Set<IConstraint> failed = new HashSet<>();
    private final Log delayedLog = new Log();
    private int reductions = 0;
    private int delays = 0;

    public ConcurrentSolver(State state, Iterable<IConstraint> constraints, Completeness completeness, IDebugContext debug) {
        this(state, constraints, completeness, v -> false, s -> false, debug);
    }
    
    public ConcurrentSolver(State state, Iterable<IConstraint> constraints, Completeness completeness, Predicate1<ITermVar> isRigid, Predicate1<ITerm> isClosed, IDebugContext debug) {
        this.initConstraints = constraints;
        this.initCompleteness = completeness;
        this.debug = debug;
        this.isRigid = isRigid;
        this.isClosed = isClosed;
        
        this.proxyDebug = new LazyDebugContext(debug);
        this.constraints = new ConcurrentConstraintStore(constraints, debug);
        this.state = state;
        this.completeness = ConcurrentCompleteness.asConcurrent(completeness);
        this.completeness.addAll(constraints);
    }
    
    @Override
    public SolverResult call() throws InterruptedException {
        while (solveStep());
        
        return finishSolver();
    }
    
    /**
     * @return
     *      true if another step is required, false otherwise
     * @throws InterruptedException
     */
    public boolean solveStep() throws InterruptedException {
        ConcurrentEntry entry = constraints.getActiveConstraint(debug);
        if (entry == null) return false;
        
        try {
            IDebugContext subDebug = proxyDebug.subContext();
            final IConstraint constraint = entry.constraint();
            if(proxyDebug.isEnabled(Level.Info)) {
                proxyDebug.info("Solving {}", constraint.toString(ConcurrentSolver.shallowTermFormatter(state.unifier())));
            }
            try {
                final Optional<ConstraintResult> maybeResult;
                maybeResult =
                        constraint.solve(state, new ConstraintContext(completeness.freeze(), isRigid, isClosed, subDebug));
                addTime(constraint, 1, successCount, debug);
                entry.remove();
                completeness.remove(constraint);
                reductions += 1;
                if(maybeResult.isPresent()) {
                    final ConstraintResult result = maybeResult.get();
                    state = result.state();
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
                    constraints.activateFromEdges(Completeness.criticalEdges(constraint, result.state()), subDebug);
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
        } finally {
            entry.done();
        }
        return true;
    }

    /**
     * Called to finish the solving.
     * 
     * @return
     * @throws InterruptedException
     */
    protected SolverResult finishSolver() throws InterruptedException {
        // invariant: there should be no remaining active constraints
        if(constraints.activeSize() > 0) {
            debug.warn("Expected no remaining active constraints, but got ", constraints.activeSize());
        }

        final Map<IConstraint, Delay> delayed = constraints.delayed();
        delayedLog.flush(debug);
        debug.info("Solved {} constraints ({} delays) with {} failed, and {} remaining constraint(s).", reductions,
                delays, failed.size(), constraints.delayedSize());
        logTimes("success", successCount, debug);
        logTimes("delay", delayCount, debug);

        return SolverResult.of(state, completeness, failed, delayed);
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

    protected Optional<SolverResult> entails(final State state, final Iterable<IConstraint> constraints,
            final Completeness completeness, final IDebugContext debug) throws InterruptedException, Delay {
        return entails(state, constraints, completeness, ImmutableSet.of(), debug);
    }

    protected Optional<SolverResult> entails(final State state, final Iterable<IConstraint> constraints,
            final Completeness completeness, final Iterable<ITermVar> _localVars, final IDebugContext debug)
            throws InterruptedException, Delay {
        if(debug.isEnabled(Level.Info)) {
            debug.info("Checking entailment of {}", toString(constraints, state.unifier()));
        }
        final Set<ITermVar> localVars = ImmutableSet.copyOf(_localVars);
        final Set<ITermVar> rigidVars = Sets.difference(state.vars(), localVars);
        //TODO Figure out what entails does, and how to implement it.
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
            debug.error(" * {}", constraint.toString(ConcurrentSolver.shallowTermFormatter(unifier)));
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
            sb.append(constraint.toString(ConcurrentSolver.shallowTermFormatter(unifier)));
        }
        return sb.toString();
    }

    public static TermFormatter shallowTermFormatter(final IUnifier.Immutable unifier) {
        return new UnifierFormatter(unifier, 3);
    }

}