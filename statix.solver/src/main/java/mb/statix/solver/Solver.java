package mb.statix.solver;

import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import org.metaborg.util.iterators.Iterables2;

import com.google.common.collect.Sets;

import mb.nabl2.terms.ITermVar;
import mb.nabl2.terms.unification.IUnifier;

public class Solver {

    public static enum Entailment {
        YES, NO, MAYBE;
    }

    private Solver() {
    }

    public static Config solve(final Config config, final IDebugContext debug) throws InterruptedException {
        debug.info("Solving constraints");

        // set-up
        final Set<IConstraint> constraints = Sets.newConcurrentHashSet(config.constraints());
        State state = config.state();
        Completeness completeness = config.completeness();
        completeness = completeness.addAll(constraints);

        // fixed point
        final Set<IConstraint> failed = Sets.newHashSet();
        boolean progress = true;
        outer: while(progress) {
            progress = false;
            final Iterator<IConstraint> it = constraints.iterator();
            while(it.hasNext()) {
                if(Thread.interrupted()) {
                    throw new InterruptedException();
                }
                final IConstraint constraint = it.next();
                debug.info("Solving {}", constraint.toString(state.unifier()));
                IDebugContext subDebug = debug.subContext();
                try {
                    Optional<Result> maybeResult = constraint.solve(state, completeness, subDebug);
                    progress = true;
                    it.remove();
                    completeness = completeness.remove(constraint);
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
                        if(debug.isRoot()) {
                            printTrace(constraint, state.unifier(), subDebug);
                        } else {
                            debug.info("Break early because of errors.");
                            break outer;
                        }
                    }
                } catch(Delay d) {
                    subDebug.info("Delayed");
                }
            }
        }

        // return
        debug.info("Solved with {} failed and {} remaining constraint(s).", failed.size(), constraints.size());
        return Config.of(state, constraints, completeness).withErrors(config.errors()).withErrors(failed);
    }

    public static boolean entails(Config config, IDebugContext debug) throws InterruptedException, Delay {
        return entails(config, Iterables2.empty(), debug);
    }

    public static boolean entails(Config config, Iterable<ITermVar> localVars, IDebugContext debug)
            throws InterruptedException, Delay {
        debug.info("Checking entailment of {}", toString(config.constraints(), config.state().unifier()));
        final State state = config.state();
        final Config result = Solver.solve(config, debug.subContext());
        if(result.hasErrors()) {
            debug.info("Constraints not entailed");
            return false;
        } else if(result.constraints().isEmpty()) {
            if(state.entails(result.state(), localVars)) {
                debug.info("Constraints entailed");
                return true;
            } else {
                debug.info("Cannot decide constraint entailment (instantiated variables)");
                throw new Delay();
            }
        } else {
            debug.info("Cannot decide constraint entailment (unsolved constraints)");
            throw new Delay();
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