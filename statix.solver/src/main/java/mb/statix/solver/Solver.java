package mb.statix.solver;

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
import com.google.common.collect.Sets;

import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.ITermVar;
import mb.nabl2.terms.unification.IUnifier;
import mb.nabl2.terms.unification.UnifierFormatter;
import mb.nabl2.util.TermFormatter;
import mb.statix.scopegraph.reference.CriticalEdge;
import mb.statix.solver.log.IDebugContext;
import mb.statix.solver.log.LazyDebugContext;
import mb.statix.solver.log.Log;
import mb.statix.solver.store.BaseConstraintStore;

public class Solver {

    private Solver() {
    }

    public static SolverResult solve(final State state, final Iterable<IConstraint> constraints,
            final Completeness completeness, final IDebugContext debug) throws InterruptedException {
        return solve(state, constraints, completeness, v -> false, s -> false, debug);
    }

    public static SolverResult solve(final State _state, final Iterable<IConstraint> _constraints,
            final Completeness _completeness, Predicate1<ITermVar> isRigid, Predicate1<ITerm> isClosed,
            final IDebugContext debug) throws InterruptedException {
        debug.info("Solving constraints");
        final LazyDebugContext proxyDebug = new LazyDebugContext(debug);

        // set-up
        final IConstraintStore constraints = new BaseConstraintStore(_constraints, debug);
        State state = _state;
        Completeness completeness = _completeness;
        completeness = completeness.addAll(_constraints);

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
            for(IConstraintStore.Entry entry : constraints.active()) {
                if(Thread.interrupted()) {
                    throw new InterruptedException();
                }
                final IConstraint constraint = entry.constraint();
                if(proxyDebug.isEnabled(Level.Info)) {
                    proxyDebug.info("Solving {}", constraint.toString(Solver.shallowTermFormatter(state.unifier())));
                }
                IDebugContext subDebug = proxyDebug.subContext();
                try {
                    Optional<ConstraintResult> maybeResult =
                            constraint.solve(state, new ConstraintContext(completeness, isRigid, isClosed, subDebug));
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
                        constraints.activateFromVars(result.vars());
                        constraints.activateFromEdges(Completeness.criticalEdges(constraint, result.state()));
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

        return SolverResult.of(state, completeness, failed, delayed);
    }

    public static Optional<SolverResult> entails(final State state, final Iterable<IConstraint> constraints,
            final Completeness completeness, final IDebugContext debug) throws InterruptedException, Delay {
        return entails(state, constraints, completeness, ImmutableSet.of(), debug);
    }

    public static Optional<SolverResult> entails(final State state, final Iterable<IConstraint> constraints,
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
    public static abstract class ASolverResult {

        @Value.Parameter public abstract State state();

        @Value.Parameter public abstract Completeness completeness();

        @Value.Parameter public abstract Set<IConstraint> errors();

        public boolean hasErrors() {
            return !errors().isEmpty();
        }

        @Value.Parameter public abstract Map<IConstraint, Delay> delays();

        public Delay delay() {
            ImmutableSet.Builder<ITermVar> vars = ImmutableSet.builder();
            ImmutableSet.Builder<CriticalEdge> scopes = ImmutableSet.builder();
            delays().values().stream().forEach(d -> {
                vars.addAll(d.vars());
                scopes.addAll(d.criticalEdges());
            });
            return new Delay(vars.build(), scopes.build());
        }

    }

    public static TermFormatter shallowTermFormatter(final IUnifier.Immutable unifier) {
        return new UnifierFormatter(unifier, 3);
    }

}