package mb.statix.solver;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import org.immutables.value.Value;
import org.metaborg.util.functions.Predicate1;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.ITermVar;
import mb.nabl2.terms.unification.IUnifier;
import mb.statix.solver.log.IDebugContext;
import mb.statix.solver.log.LazyDebugContext;
import mb.statix.solver.log.Log;

public class Solver {

    public static enum Entailment {
        YES, NO, MAYBE;
    }

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
        final Set<IConstraint> constraints = Sets.newConcurrentHashSet(_constraints);
        State state = _state;
        Completeness completeness = _completeness;
        completeness = completeness.addAll(constraints);

        // fixed point
        final Set<IConstraint> failed = Sets.newHashSet();
        final Log delayedLog = new Log();
        final Map<IConstraint, Delay> delays = Maps.newHashMap();
        boolean progress = true;
        int reduced = 0;
        int delayed = 0;
        outer: while(progress) {
            progress = false;
            delayedLog.clear();
            delays.clear();
            final Iterator<IConstraint> it = constraints.iterator();
            while(it.hasNext()) {
                if(Thread.interrupted()) {
                    throw new InterruptedException();
                }
                final IConstraint constraint = it.next();
                proxyDebug.info("Solving {}", constraint.toString(state.unifier()));
                IDebugContext subDebug = proxyDebug.subContext();
                try {
                    Optional<Result> maybeResult =
                            constraint.solve(state, new ConstraintContext(completeness, isRigid, isClosed, subDebug));
                    progress = true;
                    it.remove();
                    completeness = completeness.remove(constraint);
                    reduced += 1;
                    if(maybeResult.isPresent()) {
                        final Result result = maybeResult.get();
                        state = result.state();
                        if(!result.constraints().isEmpty()) {
                            final List<IConstraint> newConstaints = result.constraints().stream()
                                    .map(c -> c.withCause(constraint)).collect(Collectors.toList());
                            subDebug.info("Simplified to {}", toString(newConstaints, state.unifier()));
                            constraints.addAll(newConstaints);
                            completeness = completeness.addAll(newConstaints);
                        }
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
                    delays.put(constraint, d);
                    delayed += 1;
                }
            }
        }

        delayedLog.flush(debug);
        debug.info("Solved {} constraints ({} delays) with {} failed and {} remaining constraint(s).", reduced, delayed,
                failed.size(), constraints.size());

        return SolverResult.of(state, completeness, failed, delays);
    }

    public static Optional<SolverResult> entails(final State state, final Iterable<IConstraint> constraints,
            final Completeness completeness, final IDebugContext debug) throws InterruptedException, Delay {
        return entails(state, constraints, completeness, ImmutableSet.of(), debug);
    }

    public static Optional<SolverResult> entails(final State state, final Iterable<IConstraint> constraints,
            final Completeness completeness, final Iterable<ITermVar> _localVars, final IDebugContext debug)
            throws InterruptedException, Delay {
        debug.info("Checking entailment of {}", toString(constraints, state.unifier()));
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
            throw result.delay();
        }

    }

    private static void printTrace(IConstraint failed, IUnifier unifier, IDebugContext debug) {
        @Nullable IConstraint constraint = failed;
        while(constraint != null) {
            debug.error(" * {}", constraint.toString(unifier));
            constraint = constraint.cause().orElse(null);
        }
    }

    private static String toString(Iterable<IConstraint> constraints, IUnifier unifier) {
        final StringBuilder sb = new StringBuilder();
        boolean first = true;
        for(IConstraint constraint : constraints) {
            if(first) {
                first = false;
            } else {
                sb.append(", ");
            }
            sb.append(constraint.toString(unifier));
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
            ImmutableMultimap.Builder<ITerm, ITerm> scopes = ImmutableMultimap.builder();
            delays().values().stream().forEach(d -> {
                vars.addAll(d.vars());
                scopes.putAll(d.scopes());
            });
            return new Delay(vars.build(), scopes.build());
        }

    }

}