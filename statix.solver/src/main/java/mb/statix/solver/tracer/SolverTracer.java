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

import jakarta.annotation.Nullable;

/**
 * A tracer which allows observing and influencing the behavior of a Statix solver externally.
 * <p>
 * When an {@link SolverTracer} instance is passed to a {@link StatixSolver}, it will invoke its callbacks during the
 * solving process. Tracers can influence the solver by returning an alternative {@link StepResult} after each step,
 * and can aggregate meta-data, which they return with the {@link SolverTracer#result} method.
 * This method is called when constraint solving is finished. Its return value is included in the {@link SolverResult}
 * emitted by the solver.
 *
 * @param <R> the type of result this tracer emits
 */
public abstract class SolverTracer<R extends SolverTracer.IResult<R>> {

    /**
     * Attention: this method can be called by multiple threads at the same time. In addition, the returned instance can
     * be shared with a different thread, implying that all other methods can be called concurrently as well. Statefull
     * tracers should therefore do proper synchronization.
     */
    public abstract SolverTracer<R> subTracer();

    /**
     * Get the result of the tracer.
     * <p>
     * This is used to store the result of the tracer in the solver result.
     *
     * @param state the final state of the solver
     * @return the result of the tracer
     */
    public abstract R result(IState.Immutable state);

    // Constraint solving event callbacks

    /**
     * Callback that is called when the solver starts trying to solve a constraint.
     *
     * @param constraint the constraint about to be solved
     * @param state the solver state just before attempting to solve the constraint
     */
    public void onTrySolveConstraint(IConstraint constraint, IState.Immutable state) {
        // Override this implementation to handle this event
    }

    /**
     * Callback that is called when the solver performs a solving step.
     *
     * @param step the step that was performed
     * @param oldState the state of the solver before performing the step
     * @return an optional result to influence subsequent solver behavior;
     * or an empty optional to not mess with it
     */
    public Optional<StepResult> onStep(IStep step, IState.Immutable oldState) {
        return step.result().match(
            (ns, up, nc, nce, ne) -> onConstraintSolved(step.constraint(), ns, up, nc, nce, ne),
            ex -> onConstraintFailed(step.constraint(), oldState, ex),
            dl -> onConstraintDelayed(step.constraint(), oldState, dl)
        );
    }

    /**
     * Callback that is called when the solver solved a constraint.
     *
     * @param constraint the constraint that was solved
     * @param newState the state just after solving the constraint
     * @param updatedVars the variables updated as a result of solving the constraint
     * @param newConstraints the new constraints resulting from solving the constraint
     * @param newCriticalEdges the new critical edges resulting from solving the constraint
     * @param newExistentials the new existentially quantified variables resulting from solving the constraint
     * @return an optional result to influence subsequent solver behavior;
     * or an empty optional to not mess with it
     */
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

    /**
     * Callback that is called when the solver solved a constraint.
     *
     * @param constraint the constraint that was solved
     * @param newState the state just after solving the constraint
     */
    public void onConstraintSolved(IConstraint constraint, IState.Immutable newState) {
        // Override this implementation to handle this event
    }

    /**
     * Callback that is called when the solver failed to solve a constraint.
     *
     * @param constraint the constraint that was delayed
     * @param newState the state just after delaying the constraint
     * @param exception the exception that occurred; or `null`
     * @return an optional result to influence subsequent solver behavior;
     * or an empty optional to not mess with it
     */
    public Optional<StepResult> onConstraintFailed(
            IConstraint constraint,
            IState.Immutable newState,
            @Nullable Throwable exception
    ) {
        // Override this implementation to handle this event
        onConstraintFailed(constraint, newState);
        return Optional.empty();
    }

    /**
     * Callback that is called when the solver failed to solve a constraint.
     *
     * @param constraint the constraint that was delayed
     */
    public void onConstraintFailed(IConstraint constraint, IState.Immutable state) {
        // Override this implementation to handle this event
    }

    /**
     * Callback that is called when the solver delayed a constraint.
     *
     * @param constraint the constraint that was delayed
     * @param newState the state just after delaying the constraint
     * @param delay the delay
     * @return an optional result to influence subsequent solver behavior;
     * or an empty optional to not mess with it
     */
    public Optional<StepResult> onConstraintDelayed(
            IConstraint constraint,
            IState.Immutable newState,
            Delay delay
    ) {
        // Override this implementation to handle this event
        onConstraintDelayed(constraint, newState);
        return Optional.empty();
    }


    /**
     * Callback that is called when the solver delayed a constraint.
     *
     * @param constraint the constraint that was delayed
     */
    public void onConstraintDelayed(IConstraint constraint, IState.Immutable state) {
        // Override this implementation to handle this event
    }

    /**
     * Interface for a solver tracer result.
     *
     * @param <SELF> the type of the solver tracer result itself
     */
    public interface IResult<SELF extends IResult<SELF>> extends Serializable {
        /**
         * Combines this tracer result with another tracer result.
         *
         * @param other the other tracer result
         * @return the combined tracer result
         */
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
