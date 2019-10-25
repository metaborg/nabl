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
        final IUnifier.Immutable unifier = state.unifier();
        if(debug.isEnabled(Level.Info)) {
            debug.info("Checking entailment of {}", toString(constraint, unifier));
        }

        final SolverResult result = Solver.solve(state, constraint, isComplete, debug.subContext());

        if(result.hasErrors()) {
            // no entailment if errors
            debug.info("Constraints not entailed: errors");
            return false;
        }

        final IState.Immutable newState = result.state();
        final Set<ITermVar> newVars = Sets.difference(newState.vars(), state.vars());
        final Set<Scope> newScopes = Sets.difference(newState.scopes(), state.scopes());

        if(!result.delays().isEmpty()) {
            final Delay delay = result.delay().removeAll(newVars, newScopes);
            if(delay.criticalEdges().isEmpty() && delay.vars().isEmpty()) {
                debug.info("Constraints not entailed: internal stuckness"); // of the point-free mind
                return false;
            } else {
                debug.info("Cannot decide constraint entailment: unsolved constraints");
                throw delay;
            }
        }

        // NOTE The retain operation is important because it may change
        //      representatives, which can be local to newUnifier.
        final IUnifier.Immutable newUnifier = newState.unifier().removeAll(newVars).unifier();
        if(!Sets.intersection(newUnifier.freeVarSet(), newVars).isEmpty()) {
            throw new IllegalStateException("Entailment internal variables leak");
        }

        final Set<ITermVar> unifiedVars = Sets.difference(newUnifier.varSet(), unifier.varSet());
        // FIXME This test assumes the newUnifier is an extension of the old one.
        //       Without this assumption, we should use the more expensive test
        //       `newUnifier.equals(state.unifier())`
        if(!unifiedVars.isEmpty()) {
            debug.info("Cannot decide constraint entailment: unified rigid vars)");
            throw Delay.ofVars(unifiedVars);
        }

        // check that all (remaining) disequalities are implied (i.e., not unifiable) in the original unifier
        // @formatter:off
        final Collection<ITermVar> disunifiedVars = newUnifier.disequalities().stream().map(Diseq::toTuple)
                .flatMap(diseq -> diseq.apply(unifier::disunify).map(r -> r.result().varSet().stream()).orElse(Stream.empty()))
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