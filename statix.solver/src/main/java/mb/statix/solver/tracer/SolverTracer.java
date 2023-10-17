package mb.statix.solver.tracer;

import java.io.Serializable;
import java.util.Collection;
import java.util.Optional;

import io.usethesource.capsule.Map;
import io.usethesource.capsule.Set;
import mb.nabl2.terms.ITermVar;
import mb.statix.concurrent.StatixSolver;
import mb.statix.solver.Delay;
import mb.statix.solver.IConstraint;
import mb.statix.solver.IState;
import mb.statix.solver.IState.Immutable;
import mb.statix.solver.completeness.ICompleteness;
import mb.statix.solver.persistent.SolverResult;
import mb.statix.solver.persistent.step.IStep;
import mb.statix.solver.persistent.step.StepResult;

import javax.annotation.Nullable;

/**
 * Tracers allow to observe the behavior of a Statix solver externally. When an {@link SolverTracer} instance is passed
 * to a {@link StatixSolver}, it will invoke its callback at the correct positions. Tracers can aggregate meta-data,
 * which they return with the {@link SolverTracer#result()} method. This method is called when constraint solving is
 * finished. Its return value is included in the {@link SolverResult} emitted by the solver.
 *
 * @param <R>
 *            The type of result this tracer emits.
 */
public abstract class SolverTracer<R extends SolverTracer.IResult<R>> {

    /**
     * Attention: this method can be called by multiple threads at the same time. In addition, the returned instance can
     * be shared with a different thread, implying that all other methods can be called concurrently as well. Statefull
     * tracers should therefore do proper synchronization.
     */
    public abstract SolverTracer<R> subTracer();

    public abstract R result(IState.Immutable state);

    // Constraint solving event callbacks

    public void onTrySolveConstraint(IConstraint constraint, IState.Immutable state) {

        // Override this implementation to handle this event
    }

    public Optional<StepResult> onStep(IStep step, IState.Immutable oldState) {
        step.result().visit(
                (ns, up, nc, nce, ne) -> onConstraintSolved(step.constraint(), ns),
                ex -> onConstraintFailed(step.constraint(), oldState),
                dl -> onConstraintDelayed(step.constraint(), oldState)
        return step.result().match(
            (ns, up, nc, nce, ne) -> onConstraintSolved(step.constraint(), ns, up, nc, nce, ne),
            ex -> onConstraintFailed(step.constraint(), oldState, ex),
            dl -> onConstraintDelayed(step.constraint(), oldState, dl)
        );
    }
    public Optional<StepResult> onConstraintSolved(
            IConstraint constraint,
            IState.Immutable newState,
            Set.Immutable<ITermVar> updatedVars,
            Collection<IConstraint> newConstraints,
            ICompleteness.Immutable newCriticalEdges,
            Map.Immutable<ITermVar, ITermVar> newExistentials
    ) {
        // Override this implementation to handle this event
        onConstraintSolved(constraint, newState);
        return Optional.empty();
    }

    public void onConstraintSolved(IConstraint constraint, IState.Immutable state) {
    public void onConstraintSolved(IConstraint constraint, IState.Immutable newState) {
        // Override this implementation to handle this event
    }

    public Optional<StepResult> onConstraintFailed(
            IConstraint constraint,
            IState.Immutable newState,
            @Nullable Throwable exception
    ) {
        // Override this implementation to handle this event
        onConstraintFailed(constraint, newState);
        return Optional.empty();
    }

    public void onConstraintDelayed(IConstraint constraint, IState.Immutable state) {
    public void onConstraintFailed(IConstraint constraint, IState.Immutable state) {
        // Override this implementation to handle this event
    }

    public Optional<StepResult> onConstraintDelayed(
            IConstraint constraint,
            IState.Immutable newState,
            Delay delay
    ) {
        // Override this implementation to handle this event
        onConstraintDelayed(constraint, newState);
        return Optional.empty();
    }

    public void onConstraintFailed(IConstraint constraint, IState.Immutable state) {

    public void onConstraintDelayed(IConstraint constraint, IState.Immutable state) {
        // Override this implementation to handle this event
    }

    public interface IResult<SELF extends IResult<SELF>> extends Serializable {
        SELF combine(SELF other);
    }

    protected abstract class SubTracer extends SolverTracer<R> {

        @Override public SolverTracer<R> subTracer() {
            return this;
        }

        @Override public void onTrySolveConstraint(IConstraint constraint, Immutable state) {
            SolverTracer.this.onTrySolveConstraint(constraint, state);
        }

        @Override public void onConstraintSolved(IConstraint constraint, Immutable state) {
            SolverTracer.this.onConstraintSolved(constraint, state);
        }

        @Override public void onConstraintDelayed(IConstraint constraint, Immutable state) {
            SolverTracer.this.onTrySolveConstraint(constraint, state);
        }

        @Override public void onConstraintFailed(IConstraint constraint, Immutable state) {
            SolverTracer.this.onConstraintFailed(constraint, state);
        }

    }

}
