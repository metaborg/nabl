package mb.statix.solver.persistent;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import org.metaborg.util.log.Level;
import org.metaborg.util.task.ICancel;
import org.metaborg.util.task.IProgress;

import com.google.common.collect.Sets;

import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.ITermVar;
import mb.nabl2.terms.unification.UnifierFormatter;
import mb.nabl2.terms.unification.ud.IUniDisunifier;
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
import mb.statix.spec.Spec;

public class Solver {

    private Solver() {
    }

    public static SolverResult solve(final Spec spec, final IState.Immutable state, final IConstraint constraint,
            final IDebugContext debug, IProgress progress, ICancel cancel) throws InterruptedException {
        return solve(spec, state, constraint, (s, l, st) -> true, debug, progress, cancel);
    }

    public static SolverResult solve(final Spec spec, final IState.Immutable state, final IConstraint constraint,
            final IsComplete isComplete, final IDebugContext debug, IProgress progress, final ICancel cancel)
            throws InterruptedException {
        return new GreedySolver(spec, state, constraint, isComplete, debug, progress, cancel).solve();
    }

    public static SolverResult solve(final Spec spec, final IState.Immutable state,
            final Iterable<IConstraint> constraints, final Map<IConstraint, Delay> delays,
            final ICompleteness.Immutable completeness, final IDebugContext debug, IProgress progress, ICancel cancel)
            throws InterruptedException {
        return new GreedySolver(spec, state, constraints, delays, completeness, debug, progress, cancel).solve();
    }

    public static boolean entails(final Spec spec, IState.Immutable state, final IConstraint constraint,
            final IsComplete isComplete, final IDebugContext debug, IProgress progress, ICancel cancel)
            throws Delay, InterruptedException {
        final IUniDisunifier.Immutable unifier = state.unifier();
        if(debug.isEnabled(Level.Info)) {
            debug.info("Checking entailment of {}", toString(constraint, unifier));
        }

        final SolverResult result =
                Solver.solve(spec, state, constraint, isComplete, debug.subContext(), progress, cancel);
        return Solver.entails(state, result, debug);
    }

    public static boolean entails(IState.Immutable initialState, SolverResult result, final IDebugContext debug)
            throws Delay {
        final IUniDisunifier.Immutable unifier = initialState.unifier();
        if(debug.isEnabled(Level.Info)) {
            debug.info("Checking entailment");
        }

        if(result.hasErrors()) {
            // no entailment if errors
            debug.info("Constraints not entailed: errors");
            return false;
        }

        final IState.Immutable newState = result.state();
        final Set<ITermVar> newVars = Sets.difference(newState.vars(), initialState.vars());
        final Set<Scope> newScopes = Sets.difference(newState.scopes(), initialState.scopes());

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

        // NOTE The removeAll operation is important because it may change
        //      representatives, which can be local to newUnifier.
        //      After the removeAll, newUnifier.varSet should not intersect with newVars.
        final IUniDisunifier.Immutable newUnifier = newState.unifier().removeAll(newVars).unifier();
        if(!Sets.intersection(newUnifier.freeVarSet(), newVars).isEmpty()) {
            debug.info("Constraints not entailed: internal variables leak");
            return false;
        }

        final Set<ITermVar> unifiedVars = Sets.difference(newUnifier.varSet(), unifier.varSet());
        // FIXME This test assumes the newUnifier is an extension of the old one.
        //       Without this assumption, we should use the more expensive test
        //       `newUnifier.equals(state.unifier())`
        if(!unifiedVars.isEmpty()) {
            debug.info("Cannot decide constraint entailment: unified rigid vars)");
            throw Delay.ofVars(unifiedVars);
        }

        // check that all (remaining) disequalities are implied (i.e., not unifiable) in the original unifier,
        // which is the case if disunify succeeds with no remaining disequality
        // @formatter:off
        final List<ITermVar> disunifiedVars = newUnifier.disequalities().stream()
                .filter(diseq -> diseq.toTuple().apply(unifier::disunify).map(r -> r.result().isPresent()).orElse(true))
                .flatMap(diseq -> diseq.varSet().stream())
                .collect(Collectors.toList());
        // @formatter:on
        if(!disunifiedVars.isEmpty()) {
            debug.info("Cannot decide constraint entailment: disunified rigid vars)");
            throw Delay.ofVars(disunifiedVars);
        }

        debug.info("Constraints entailed");
        return true;
    }

    static void printTrace(IConstraint failed, IUniDisunifier.Immutable unifier, IDebugContext debug) {
        @Nullable IConstraint constraint = failed;
        while(constraint != null) {
            debug.error(" * {}", constraint.toString(Solver.shallowTermFormatter(unifier)));
            constraint = constraint.cause().orElse(null);
        }
    }

    public static String toString(IConstraint constraint, IUniDisunifier.Immutable unifier) {
        return constraint.toString(Solver.shallowTermFormatter(unifier));
    }

    public static String toString(Iterable<IConstraint> constraints, IUniDisunifier.Immutable unifier) {
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

    public static TermFormatter shallowTermFormatter(final IUniDisunifier unifier) {
        return new UnifierFormatter(unifier, 4);
    }

}