package mb.statix.solver;

import java.util.Iterator;
import java.util.Optional;
import java.util.Set;

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

    public static Config solve(Config config, IDebugContext debug) throws InterruptedException {
        debug.info("Solving constraints");

        // set-up
        final Set<IConstraint> constraints = Sets.newConcurrentHashSet(config.constraints());
        State state = config.state();
        Completeness completeness = config.completeness();
        completeness = completeness.addAll(constraints);

        // if not root, reset errors, because we want to short-cut only on errors introduced by the
        // guard constraints, not on errors pre-existing in the state
        if(!debug.isRoot()) {
            state = state.withErroneous(false);
        }

        // fixed point
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
                Optional<Result> maybeResult = constraint.solve(state, completeness, subDebug);
                if(maybeResult.isPresent()) {
                    progress = true;
                    it.remove();
                    final Result result = maybeResult.get();
                    state = result.state();
                    completeness = completeness.remove(constraint);
                    if(!debug.isRoot() && state.isErroneous()) {
                        debug.info("Break early because of errors.");
                        break outer;
                    }
                    if(!result.constraints().isEmpty()) {
                        subDebug.info("Simplified to {}", toString(result.constraints(), state.unifier()));
                        constraints.addAll(result.constraints());
                        completeness = completeness.addAll(result.constraints());
                    }
                } else {
                    subDebug.info("Delayed");
                }
            }
        }

        // return
        debug.info("Solved {} errors and {} remaining constraints.", state.isErroneous() ? "with" : "without",
                constraints.size());
        return Config.of(state, constraints, completeness);
    }

    public static Optional<Boolean> entails(Config config, IDebugContext debug) throws InterruptedException {
        return entails(config, Iterables2.empty(), debug);
    }

    public static Optional<Boolean> entails(Config config, Iterable<ITermVar> localVars, IDebugContext debug)
            throws InterruptedException {
        debug.info("Checking entailment of {}", toString(config.constraints(), config.state().unifier()));
        final State state = config.state();
        final Config result = Solver.solve(config, debug.subContext());
        if(result.state().isErroneous()) {
            debug.info("Constraints not entailed");
            return Optional.of(false);
        } else if(result.constraints().isEmpty()) {
            if(state.entails(result.state(), localVars)) {
                debug.info("Constraints entailed");
                return Optional.of(true);
            } else {
                debug.info("Cannot decide constraint entailment (instantiated variables)");
                return Optional.empty();
            }
        } else {
            debug.info("Cannot decide constraint entailment (unsolved constraints)");
            return Optional.empty();
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