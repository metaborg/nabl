package mb.statix.solver.persistent;

import java.util.Map;
import java.util.Set;

import javax.annotation.Nullable;

import org.metaborg.util.log.Level;
import org.metaborg.util.task.ICancel;
import org.metaborg.util.task.IProgress;

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
import mb.statix.solver.IState.Immutable;
import mb.statix.solver.completeness.ICompleteness;
import mb.statix.solver.completeness.IsComplete;
import mb.statix.solver.log.IDebugContext;
import mb.statix.spec.Spec;

public class Solver {

    private Solver() {
    }

    public static SolverResult solve(final Spec spec, final IState.Immutable state, final IConstraint constraint,
            final IDebugContext debug, ICancel cancel, IProgress progress) throws InterruptedException {
        return solve(spec, state, constraint, (s, l, st) -> true, debug, cancel, progress);
    }

    public static SolverResult solve(final Spec spec, final IState.Immutable state, final IConstraint constraint,
            final IsComplete isComplete, final IDebugContext debug, final ICancel cancel, IProgress progress)
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
            debug.debug("Checking entailment of {}", toString(constraint, unifier));
        }

        final Immutable subState = state.subState();
        final SolverResult result =
                Solver.solve(spec, subState, constraint, isComplete, debug.subContext(), cancel, progress);
        return Solver.entailed(subState, result, debug);
    }

    public static boolean entailed(IState.Immutable initialState, SolverResult result, final IDebugContext debug)
            throws Delay {
        if(!initialState.vars().isEmpty() || !initialState.scopes().isEmpty()) {
            throw new IllegalArgumentException("Incurrent initial state: create with IState::subState.");
        }

        if(debug.isEnabled(Level.Info)) {
            debug.debug("Checking entailment");
        }

        if(result.hasErrors()) {
            // no entailment if errors
            debug.debug("Constraints not entailed: errors");
            return false;
        }

        final IState.Immutable newState = result.state();
        final Set<ITermVar> newVars = newState.vars();
        final Set<Scope> newScopes = newState.scopes();

        if(!result.delays().isEmpty()) {
            final Delay delay = result.delay().removeAll(newVars, newScopes);
            if(delay.criticalEdges().isEmpty() && delay.vars().isEmpty()) {
                debug.debug("Constraints not entailed: internal stuckness"); // of the point-free mind
                return false;
            } else {
                debug.debug("Cannot decide constraint entailment: unsolved constraints");
                throw delay;
            }
        }

        debug.debug("Constraints entailed");
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