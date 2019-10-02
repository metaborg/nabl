package mb.statix.solver.persistent;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.Nullable;

import org.metaborg.util.log.Level;

import com.google.common.collect.Sets;

import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.ITermVar;
import mb.nabl2.terms.unification.Diseq;
import mb.nabl2.terms.unification.IUnifier;
import mb.nabl2.terms.unification.UnifierFormatter;
import mb.nabl2.util.TermFormatter;
import mb.statix.scopegraph.INameResolution;
import mb.statix.scopegraph.reference.FastNameResolution;
import mb.statix.scopegraph.terms.Scope;
import mb.statix.solver.Delay;
import mb.statix.solver.IConstraint;
import mb.statix.solver.IState;
import mb.statix.solver.completeness.ICompleteness;
import mb.statix.solver.completeness.IsComplete;
import mb.statix.solver.log.IDebugContext;

public class Solver {

    private Solver() {
    }

    public static SolverResult solve(final IState.Immutable state, final IConstraint constraint,
            final IDebugContext debug) throws InterruptedException {
        return solve(state, constraint, (s, l, st) -> true, debug);
    }

    public static SolverResult solve(final IState.Immutable state, final IConstraint constraint,
            final IsComplete isComplete, final IDebugContext debug) throws InterruptedException {
        return new GreedySolver(state, constraint, isComplete, debug).solve();
        //return new StepSolver(state, constraint, isComplete, debug).solve();
    }

    public static SolverResult solve(final IState.Immutable state, final Iterable<IConstraint> constraints,
            final Map<IConstraint, Delay> delays, final ICompleteness.Immutable completeness, final IDebugContext debug)
            throws InterruptedException {
        return new GreedySolver(state, constraints, delays, completeness, debug).solve();
    }

    public static boolean entails(IState.Immutable state, final IConstraint constraint, final IsComplete isComplete,
            final IDebugContext debug) throws Delay, InterruptedException {
        if(debug.isEnabled(Level.Info)) {
            debug.info("Checking entailment of {}", toString(constraint, state.unifier()));
        }

        final SolverResult result = Solver.solve(state, constraint, isComplete, debug.subContext());

        if(result.hasErrors()) {
            // no entailment if errors
            debug.info("Constraints not entailed: errors");
            return false;
        }

        if(!result.delays().isEmpty()) {
            debug.info("Cannot decide constraint entailment: unsolved constraints");
            // FIXME Can this result in an empty delay? If so, it means there's no
            //       recovering, because the delays are internal to the constraint,
            //       and unaffected by outside variables -> return false.
            throw result.delay().retainAll(state.vars(), state.scopes());
        }

        final IState.Immutable newState = result.state();
        // NOTE The retain operation is important because it may change
        //      representatives, which can be local to newUnifier.
        final IUnifier.Immutable newUnifier = newState.unifier().retainAll(state.vars()).unifier();

        final Set<ITermVar> unifiedVars = Sets.difference(newUnifier.varSet(), state.unifier().varSet());
        // FIXME This test assumes the newUnifier is an extension of the old one.
        //       Without this assumption, we should use the more expensive test
        //       `newUnifier.equals(state.unifier())`
        if(!unifiedVars.isEmpty()) {
            debug.info("Cannot decide constraint entailment: unified rigid vars)");
            throw Delay.ofVars(unifiedVars);
        }

        // @formatter:off
        final Collection<ITermVar> disunifiedVars = newUnifier.disequalities().stream().map(Diseq::toTuple)
                .filter(diseq -> diseq.apply((t1, t2) -> state.unifier().diff(t1, t2).isPresent()))
                .flatMap(diseq -> diseq.apply((t1, t2) -> Stream.concat(t1.getVars().stream(), t2.getVars().stream())))
                .collect(Collectors.toList());
        // @formatter:on
        if(!disunifiedVars.isEmpty()) {
            debug.info("Cannot decide constraint entailment: disunified rigid vars)");
            throw Delay.ofVars(disunifiedVars);
        }

        debug.info("Constraints entailed");
        return true;
    }

    static void printTrace(IConstraint failed, IUnifier.Immutable unifier, IDebugContext debug) {
        @Nullable IConstraint constraint = failed;
        while(constraint != null) {
            debug.error(" * {}", constraint.toString(Solver.shallowTermFormatter(unifier)));
            constraint = constraint.cause().orElse(null);
        }
    }

    static String toString(IConstraint constraint, IUnifier.Immutable unifier) {
        return constraint.toString(Solver.shallowTermFormatter(unifier));
    }

    static String toString(Iterable<IConstraint> constraints, IUnifier.Immutable unifier) {
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

    public static INameResolution.Builder<Scope, ITerm, ITerm> nameResolutionBuilder() {
        return FastNameResolution.builder();

    }

    public static TermFormatter shallowTermFormatter(final IUnifier unifier) {
        return new UnifierFormatter(unifier, 3);
    }

}