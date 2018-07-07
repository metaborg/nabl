package mb.statix.solver;

import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import org.metaborg.util.functions.Predicate1;

import com.google.common.collect.ImmutableSet;
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

    public static Config solve(final Config config, final IDebugContext debug) throws InterruptedException {
        return solve(config, v -> false, s -> false, debug);
    }

    public static Config solve(final Config config, Predicate1<ITermVar> isRigid, Predicate1<ITerm> isClosed,
            final IDebugContext debug) throws InterruptedException {
        debug.info("Solving constraints");
        final LazyDebugContext proxyDebug = new LazyDebugContext(debug);

        // set-up
        final Set<IConstraint> constraints = Sets.newConcurrentHashSet(config.constraints());
        State state = config.state();
        Completeness completeness = config.completeness();
        completeness = completeness.addAll(constraints);

        // fixed point
        final Set<IConstraint> failed = Sets.newHashSet();
        final Log delayedLog = new Log();
        final Set<Delay> delays = Sets.newHashSet();
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
                    delays.add(d);
                    delayed += 1;
                }
            }
        }

        delayedLog.flush(debug);
        debug.info("Solved {} constraints ({} delays) with {} failed and {} remaining constraint(s).", reduced, delayed,
                failed.size(), constraints.size());

        return Config.of(state, constraints, completeness).withErrors(config.errors()).withErrors(failed)
                .withDelays(delays);
    }

    public static boolean entails(Config config, IDebugContext debug) throws InterruptedException, Delay {
        return entails(config, ImmutableSet.of(), debug);
    }

    public static boolean entails(Config config, Iterable<ITermVar> _localVars, IDebugContext debug)
            throws InterruptedException, Delay {
        debug.info("Checking entailment of {}", toString(config.constraints(), config.state().unifier()));
        final Set<ITermVar> localVars = ImmutableSet.copyOf(_localVars);
        final State state = config.state();
        final Set<ITermVar> rigidVars = Sets.difference(state.vars(), localVars);
        final Config result = Solver.solve(config, rigidVars::contains, state.scopes()::contains, debug.subContext());
        if(result.hasErrors()) {
            debug.info("Constraints not entailed");
            return false;
        } else if(result.delays().isEmpty()) {
            debug.info("Constraints entailed");
            return true;
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

}