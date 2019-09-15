package mb.statix.solver.persistent;

import java.util.Set;

import javax.annotation.Nullable;

import org.metaborg.util.log.Level;

import com.google.common.collect.Sets;

import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.ITermVar;
import mb.nabl2.terms.unification.IUnifier;
import mb.nabl2.terms.unification.UnifierFormatter;
import mb.nabl2.util.TermFormatter;
import mb.statix.scopegraph.INameResolution;
import mb.statix.scopegraph.reference.FastNameResolution;
import mb.statix.scopegraph.terms.Scope;
import mb.statix.solver.Delay;
import mb.statix.solver.IConstraint;
import mb.statix.solver.IState;
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
        return new GreedySolver(state, isComplete, debug).solve(constraint);
        //return new StepSolver(state, isComplete, debug).solve(constraint);
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

        final IState.Immutable newState = result.state();
        final IUnifier.Immutable newUnifier = newState.unifier().retainAll(state.vars()).unifier();
        Set<ITermVar> touchedVars = Sets.difference(newUnifier.varSet(), state.unifier().varSet());
        // FIXME This test assumes the newUnifier is an extension of the old one.
        //       Without this assumption, we should to the more expensive test
        //       `newUnifier.equals(state.unifier())`
        if(!touchedVars.isEmpty()) {
            throw Delay.ofVars(touchedVars);
        }

        // FIXME Any disequalities on rigid vars introduced during solving are
        //       currently ignored. We could test as follows. Remove all disequalities
        //       in state from newState. Any remaining disequalities are
        //       (a) reductions of diseq in state, or
        //       (b) new diseq.
        //       In case (a), this can only happen if a rigid var was unified, so
        //       this is caught by the previous check. In case (b), this is a new
        //       inequality, and entailment should not hold. It is a little more
        //       complicated than that, because, if the new diseq in newState is
        //       implied by all diseq in state, entailment does hold. But it might
        //       okay to ignore this situation?

        if(!result.delays().isEmpty()) {
            debug.info("Cannot decide constraint entailment (unsolved constraints)");
            // FIXME Can this result in empty delay? If so, it means there's no recovering,
            //       because the delays are internal to the constraint, and unaffected by
            //       outside variables -> return false.
            throw result.delay().retainAll(state.vars(), state.scopes());
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