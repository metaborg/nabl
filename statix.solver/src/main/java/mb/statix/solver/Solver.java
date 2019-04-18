package mb.statix.solver;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import org.immutables.serial.Serial;
import org.immutables.value.Value;
import org.metaborg.util.functions.Predicate3;
import org.metaborg.util.log.Level;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.ITermVar;
import mb.nabl2.terms.unification.IUnifier;
import mb.nabl2.terms.unification.UnifierFormatter;
import mb.nabl2.terms.unification.IUnifier.Immutable;
import mb.nabl2.util.CapsuleUtil;
import mb.nabl2.util.TermFormatter;
import mb.statix.solver.log.IDebugContext;
import mb.statix.solver.log.LazyDebugContext;
import mb.statix.solver.log.Log;
import mb.statix.solver.store.BaseConstraintStore;

public class Solver {

    private Solver() {
    }

    public static SolverResult solve(final State _state, final Iterable<IConstraint> _constraints,
            final IDebugContext debug) throws InterruptedException {
        return solve(_state, _constraints, (s, l, st) -> true, debug);
    }

    public static SolverResult solve(final State _state, final Iterable<IConstraint> _constraints,
            final Predicate3<ITerm, ITerm, State> _isComplete, final IDebugContext debug) throws InterruptedException {
        debug.info("Solving constraints");
        final LazyDebugContext proxyDebug = new LazyDebugContext(debug);

        // set-up
        final IConstraintStore constraints = new BaseConstraintStore(_constraints, debug);
        State state = _state;
        Completeness completeness = new Completeness();
        completeness = completeness.addAll(_constraints);

        // time log
        final Map<Class<? extends IConstraint>, Long> successCount = Maps.newHashMap();
        final Map<Class<? extends IConstraint>, Long> delayCount = Maps.newHashMap();

        // fixed point
        final Set<IConstraint> failed = Sets.newHashSet();
        final Log delayedLog = new Log();
        boolean progress = true;
        int reductions = 0;
        int delays = 0;
        outer: while(progress) {
            progress = false;
            constraints.activateStray();
            delayedLog.clear();
            for(IConstraintStore.Entry entry : constraints.active(debug)) {
                if(Thread.interrupted()) {
                    throw new InterruptedException();
                }
                IDebugContext subDebug = proxyDebug.subContext();
                final IConstraint constraint = entry.constraint();
                if(proxyDebug.isEnabled(Level.Info)) {
                    proxyDebug.info("Solving {}", constraint.toString(Solver.shallowTermFormatter(state.unifier())));
                }
                try {
                    final Completeness cc = completeness;
                    final Predicate3<ITerm, ITerm, State> isComplete =
                            (s, l, st) -> cc.isComplete(s, l, st) && _isComplete.test(s, l, st);
                    final ConstraintContext params = new ConstraintContext(isComplete, subDebug);
                    final Optional<ConstraintResult> maybeResult;
                    maybeResult = new StepSolver(state, params).solve(constraint);
                    addTime(constraint, 1, successCount, debug);
                    progress = true;
                    entry.remove();
                    completeness = completeness.remove(constraint);
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
                            completeness = completeness.addAll(newConstaints);
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
                            break outer;
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
            }
        }

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

        return SolverResult.of(state, failed, delayed);
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

    public static Optional<SolverResult> entails(final State state, final Iterable<IConstraint> constraints,
            final Predicate3<ITerm, ITerm, State> isComplete, final IDebugContext debug)
            throws InterruptedException, Delay {
        return entails(state, constraints, isComplete, ImmutableSet.of(), debug);
    }

    /**
     * Determines if the given set of constraints are satisfied in the given solution,
     * regardless of how free variables are instantiated.
     * 
     * @param state
     *      the state
     * @param constraints
     *      the constraints to check
     * @param completeness
     *      the completeness to use
     * @param _localVars
     *      the variables that are local
     * @param debug
     * @return
     * @throws InterruptedException
     * @throws Delay
     */
    public static Optional<SolverResult> entails(final State state, final Iterable<IConstraint> constraints,
            final Predicate3<ITerm, ITerm, State> isComplete, final Iterable<ITermVar> _localVars,
            final IDebugContext debug) throws InterruptedException, Delay {
        if(debug.isEnabled(Level.Info)) {
            debug.info("Checking entailment of {}", toString(constraints, state.unifier()));
        }
        // remove all previously created variables/scopes to make them rigid/closed
        final State _state = state.retainVarsAndClearScopes(CapsuleUtil.toSet(_localVars));
        final SolverResult result = Solver.solve(_state, constraints, isComplete, debug.subContext());
        if(result.hasErrors()) {
            debug.info("Constraints not entailed");
            return Optional.empty();
        } else if(result.delays().isEmpty()) {
            debug.info("Constraints entailed");
            return Optional.of(result);
        } else {
            debug.info("Cannot decide constraint entailment (unsolved constraints)");
            // FIXME this doesn't remove rigid vars, as they are not part of State.vars()
            throw result.delay().removeAll(result.state().vars(), result.state().scopes());
        }

    }

    private static void printTrace(IConstraint failed, IUnifier.Immutable unifier, IDebugContext debug) {
        @Nullable IConstraint constraint = failed;
        while(constraint != null) {
            debug.error(" * {}", constraint.toString(Solver.shallowTermFormatter(unifier)));
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
            sb.append(constraint.toString(Solver.shallowTermFormatter(unifier)));
        }
        return sb.toString();
    }


    @Value.Immutable
    @Serial.Version(42L)
    public static abstract class ASolverResult implements ISolverResult {

        @Value.Parameter public abstract State state();

        @Value.Parameter public abstract Set<IConstraint> errors();

        @Value.Parameter public abstract Map<IConstraint, Delay> delays();
        
        @Override
        public Immutable unifier() {
            return state().unifier();
        }
    }

    public static TermFormatter shallowTermFormatter(final IUnifier.Immutable unifier) {
        return new UnifierFormatter(unifier, 3);
    }

}