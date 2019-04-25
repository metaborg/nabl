package mb.statix.solver;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.annotation.Nullable;

import org.immutables.serial.Serial;
import org.immutables.value.Value;
import org.metaborg.util.log.Level;

import com.google.common.collect.ImmutableSet;

import mb.nabl2.terms.ITermVar;
import mb.nabl2.terms.unification.IUnifier;
import mb.nabl2.terms.unification.UnifierFormatter;
import mb.nabl2.util.CapsuleUtil;
import mb.nabl2.util.TermFormatter;
import mb.statix.scopegraph.reference.CriticalEdge;
import mb.statix.solver.completeness.IsComplete;
import mb.statix.solver.log.IDebugContext;
import mb.statix.solver.SolverResult;
import mb.statix.solver.State;

public class Solver {

    private Solver() {
    }

    public static SolverResult solve(final State _state, final Iterable<IConstraint> _constraints,
            final IDebugContext debug) throws InterruptedException {
        return solve(_state, _constraints, (s, l, st) -> true, debug);
    }

    public static SolverResult solve(final State _state, final Iterable<IConstraint> _constraints,
            final IsComplete _isComplete, final IDebugContext debug) throws InterruptedException {
        return new GreedySolver(_state, _isComplete, debug).solve(_constraints);
        //return new StepSolver(_state, _isComplete, debug).solve(_constraints);
    }

    public static Optional<SolverResult> entails(final State state, final Iterable<IConstraint> constraints,
            final IsComplete isComplete, final IDebugContext debug) throws InterruptedException, Delay {
        return entails(state, constraints, isComplete, ImmutableSet.of(), debug);
    }

    public static Optional<SolverResult> entails(final State state, final Iterable<IConstraint> constraints,
            final IsComplete isComplete, final Iterable<ITermVar> _localVars, final IDebugContext debug)
            throws InterruptedException, Delay {
        if(debug.isEnabled(Level.Info)) {
            debug.info("Checking entailment of {}", toString(constraints, state.unifier()));
        }
        // remove all previously created variables/scopes to make them rigid/closed
        final State _state = state.retainVarsAndClearScopes(CapsuleUtil.toSet(_localVars));
        final SolverResult result = Solver.solve(_state, constraints, isComplete, debug.subContext());
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


    @Value.Immutable
    @Serial.Version(42L)
    public static abstract class ASolverResult {

        @Value.Parameter public abstract State state();

        @Value.Parameter public abstract List<IConstraint> errors();

        public boolean hasErrors() {
            return !errors().isEmpty();
        }

        @Value.Parameter public abstract Map<IConstraint, Delay> delays();

        public Delay delay() {
            ImmutableSet.Builder<ITermVar> vars = ImmutableSet.builder();
            ImmutableSet.Builder<CriticalEdge> scopes = ImmutableSet.builder();
            delays().values().stream().forEach(d -> {
                vars.addAll(d.vars());
                scopes.addAll(d.criticalEdges());
            });
            return new Delay(vars.build(), scopes.build());
        }

    }

    public static TermFormatter shallowTermFormatter(final IUnifier unifier) {
        return new UnifierFormatter(unifier, 3);
    }

}