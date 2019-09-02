package mb.statix.solver.persistent;

import java.util.Optional;

import javax.annotation.Nullable;

import org.metaborg.util.log.Level;

import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.unification.IUnifier;
import mb.nabl2.terms.unification.UnifierFormatter;
import mb.nabl2.util.TermFormatter;
import mb.statix.scopegraph.INameResolution;
import mb.statix.scopegraph.reference.FastNameResolution;
import mb.statix.scopegraph.terms.Scope;
import mb.statix.solver.Delay;
import mb.statix.solver.IConstraint;
import mb.statix.solver.completeness.IsComplete;
import mb.statix.solver.log.IDebugContext;

public class Solver {

    private Solver() {
    }

    public static SolverResult solve(final State state, final IConstraint constraint, final IDebugContext debug)
            throws InterruptedException {
        return solve(state, constraint, (s, l, st) -> true, debug);
    }

    public static SolverResult solve(final State state, final IConstraint constraint, final IsComplete isComplete,
            final IDebugContext debug) throws InterruptedException {
        return new GreedySolver(state, isComplete, debug).solve(constraint);
        //return new StepSolver(state, isComplete, debug).solve(constraint);
    }

    public static Optional<SolverResult> entails(State state, final IConstraint constraint, final IsComplete isComplete,
            final IDebugContext debug) throws InterruptedException, Delay {
        if(debug.isEnabled(Level.Info)) {
            debug.info("Checking entailment of {}", toString(constraint, state.unifier()));
        }
        // remove all previously created variables/scopes to make them rigid/closed
        state = state.clearVarsAndScopes();
        final SolverResult result = Solver.solve(state, constraint, isComplete, debug.subContext());
        if(result.hasErrors()) {
            debug.info("Constraints not entailed");
            return Optional.empty();
        } else if(result.delays().isEmpty()) {
            debug.info("Constraints entailed");
            return Optional.of(result);
        } else {
            debug.info("Cannot decide constraint entailment (unsolved constraints)");
            // FIXME this doesn't remove rigid vars, as they are not part of State.vars()
            throw result.delay().removeAll(result.state().vars(), result.state().scopes());
        }

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