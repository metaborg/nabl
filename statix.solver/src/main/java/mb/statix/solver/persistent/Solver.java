package mb.statix.solver.persistent;

import static mb.nabl2.terms.matching.TermMatch.M;

import java.util.Map;
import java.util.Set;

import javax.annotation.Nullable;

import org.metaborg.util.log.Level;
import org.metaborg.util.task.ICancel;
import org.metaborg.util.task.IProgress;

import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.ITermVar;
import mb.nabl2.terms.stratego.TermIndex;
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

    public static final int RETURN_ON_FIRST_ERROR = 1;

    public static final int TERM_FORMAT_DEPTH = 4;

    public static final boolean INCREMENTAL_CRITICAL_EDGES = true;

    private Solver() {
    }

    public static SolverResult solve(final Spec spec, final IState.Immutable state, final IConstraint constraint,
            final IDebugContext debug, ICancel cancel, IProgress progress, int flags) throws InterruptedException {
        return solve(spec, state, constraint, (s, l, st) -> true, debug, cancel, progress, flags);
    }

    public static SolverResult solve(final Spec spec, final IState.Immutable state, final IConstraint constraint,
            final IsComplete isComplete, final IDebugContext debug, final ICancel cancel, IProgress progress, int flags)
            throws InterruptedException {
        return new GreedySolver(spec, state, constraint, isComplete, debug, progress, cancel, flags).solve();
    }

    public static SolverResult solve(final Spec spec, final IState.Immutable state,
            final Iterable<IConstraint> constraints, final Map<IConstraint, Delay> delays,
            final ICompleteness.Immutable completeness, final IsComplete isComplete, final IDebugContext debug,
            IProgress progress, ICancel cancel, int flags) throws InterruptedException {
        return new GreedySolver(spec, state, constraints, delays, completeness, isComplete, debug, progress, cancel,
                flags).solve();
    }

    public static boolean entails(final Spec spec, IState.Immutable state, final Iterable<IConstraint> constraints,
            final Map<IConstraint, Delay> delays, final ICompleteness.Immutable completeness,
            final IsComplete isComplete, final IDebugContext debug, IProgress progress, ICancel cancel)
            throws Delay, InterruptedException {
        final IUniDisunifier.Immutable unifier = state.unifier();
        if(debug.isEnabled(Level.Debug)) {
            debug.debug("Checking entailment of {}", toString(constraints, unifier));
        }

        final IState.Immutable subState = state.subState();
        final SolverResult result = Solver.solve(spec, subState, constraints, delays, completeness, isComplete,
                debug.subContext(), progress, cancel, RETURN_ON_FIRST_ERROR);
        return Solver.entailed(subState, result, debug);
    }

    public static boolean entails(final Spec spec, IState.Immutable state, final IConstraint constraint,
            final IsComplete isComplete, final IDebugContext debug, IProgress progress, ICancel cancel)
            throws Delay, InterruptedException {
        final IUniDisunifier.Immutable unifier = state.unifier();
        if(debug.isEnabled(Level.Debug)) {
            debug.debug("Checking entailment of {}", toString(constraint, unifier));
        }

        final IState.Immutable subState = state.subState();
        final SolverResult result = Solver.solve(spec, subState, constraint, isComplete, debug.subContext(), cancel,
                progress, RETURN_ON_FIRST_ERROR);
        return Solver.entailed(subState, result, debug);
    }

    public static boolean entailed(IState.Immutable initialState, SolverResult result, final IDebugContext debug)
            throws Delay {
        if(!initialState.vars().isEmpty() || !initialState.scopes().isEmpty()) {
            throw new IllegalArgumentException("Incurrent initial state: create with IState::subState.");
        }

        debug.debug("Checking entailment");

        if(result.hasErrors()) {
            // no entailment if errors
            debug.debug("Constraints not entailed: errors");
            return false;
        }

        final IState.Immutable newState = result.state();
        final Set<ITermVar> newVars = newState.vars();
        final Set<Scope> newScopes = newState.scopes();
        final IUniDisunifier.Immutable newUnifier = newState.unifier();

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

        if(newUnifier.disequalities().stream().flatMap(diseq -> diseq.domainSet().stream()).filter(newVars::contains)
                .count() > 0) {
            // if any local variables are still constrained, the entailment does not hold for all possible assignments for the free local variables
            debug.debug("Constraints not entailed: internal stuckness"); // of the point-free mind
            return false;
        }

        debug.debug("Constraints entailed");
        return true;
    }

    static void printTrace(IConstraint failed, IUniDisunifier.Immutable unifier, IDebugContext debug) {
        @Nullable IConstraint constraint = failed;
        while(constraint != null) {
            debug.error(" * {}", constraint.toString(Solver.shallowTermFormatter(unifier, TERM_FORMAT_DEPTH)));
            constraint = constraint.cause().orElse(null);
        }
    }

    public static String toString(IConstraint constraint, IUniDisunifier.Immutable unifier) {
        return constraint.toString(Solver.shallowTermFormatter(unifier, TERM_FORMAT_DEPTH));
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
            sb.append(constraint.toString(Solver.shallowTermFormatter(unifier, TERM_FORMAT_DEPTH)));
        }
        return sb.toString();
    }

    public static INameResolution.Builder<Scope, ITerm, ITerm> nameResolutionBuilder() {
        return FastNameResolution.builder();

    }

    public static TermFormatter shallowTermFormatter(final IUniDisunifier unifier, int depth) {
        // @formatter:off
        return new UnifierFormatter(unifier, depth, (t, u, f) -> M.cases(
            Scope.matcher(),
            TermIndex.matcher()
        ).map(ITerm::toString).match(t, unifier));
        // @formatter:on
    }

}