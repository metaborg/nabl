package mb.statix.solver.persistent.step;

import io.usethesource.capsule.Map;
import io.usethesource.capsule.Set;
import mb.nabl2.terms.ITermVar;
import mb.statix.solver.Delay;
import mb.statix.solver.IConstraint;
import mb.statix.solver.IState;
import mb.statix.solver.completeness.ICompleteness;
import mb.statix.solver.persistent.Solver;
import org.metaborg.util.functions.*;
import org.metaborg.util.unit.Unit;

import jakarta.annotation.Nullable;
import java.util.Collection;

public abstract class StepResult {

    public abstract <T, E extends Throwable> T match(
            OnSuccess<T, E> onSuccess,
            Function1<Throwable, T> onFailure,
            Function1<Delay, T> onDelay
    ) throws E;

    public void visit(OnSuccessAction onSuccess, Action1<Throwable> onFailure, Action1<Delay> onDelay) {
        this.match((newState, updatedVars, newConstraints, newCompleteness, newExistentials) -> {
            onSuccess.apply(newState, updatedVars, newConstraints, newCompleteness, newExistentials);
            return Unit.unit;
        }, ex -> {
            onFailure.apply(ex);
            return Unit.unit;
        }, delay -> {
            onDelay.apply(delay);
            return Unit.unit;
        });
    }

    public static Success success(IState.Immutable newState) {
        return new Success(newState);
    }

    public static StepResult failure(@Nullable Throwable exception) {
        return new Failure(exception);
    }

    public static StepResult failure() {
        return new Failure(null);
    }

    public static StepResult delay(Delay delay) {
        return new Delayed(delay);
    }

    public static class Success extends StepResult {

        private final IState.Immutable newState;

        private Success(IState.Immutable newState, Set.Immutable<ITermVar> updatedVars,
                        Collection<IConstraint> newConstraints, ICompleteness.Immutable newCriticalEdges,
                        Map.Immutable<ITermVar, ITermVar> newExistentials) {
            this.newState = newState;
            this.updatedVars = updatedVars;
            this.newConstraints = newConstraints;
            this.newCriticalEdges = newCriticalEdges;
            this.newExistentials = newExistentials;
        }

        private Success(IState.Immutable newState) {
            this(newState, Solver.NO_UPDATED_VARS, Solver.NO_NEW_CONSTRAINTS, Solver.NO_NEW_CRITICAL_EDGES,
                    Solver.NO_EXISTENTIALS);
        }

        private final Set.Immutable<ITermVar> updatedVars;

        private final Collection<IConstraint> newConstraints;

        private final ICompleteness.Immutable newCriticalEdges;

        private final Map.Immutable<ITermVar, ITermVar> newExistentials;

        public IState.Immutable newState() {
            return newState;
        }

        public Set.Immutable<ITermVar> updatedVars() {
            return updatedVars;
        }

        public Collection<IConstraint> newConstraints() {
            return newConstraints;
        }

        public ICompleteness.Immutable newCriticalEdges() {
            return newCriticalEdges;
        }

        public Map.Immutable<ITermVar, ITermVar> newExistentials() {
            return newExistentials;
        }

        public Success withNewState(IState.Immutable newState) {
            return new Success(newState);
        }

        public Success withUpdatedVars(Set.Immutable<ITermVar> updatedVars) {
            return new Success(newState, updatedVars, newConstraints, newCriticalEdges, newExistentials);
        }

        public Success withNewConstraints(Collection<IConstraint> newConstraints) {
            return new Success(newState, updatedVars, newConstraints, newCriticalEdges, newExistentials);
        }

        public Success withNewCriticalEdges(ICompleteness.Immutable newCriticalEdges) {
            return new Success(newState, updatedVars, newConstraints, newCriticalEdges, newExistentials);
        }

        public Success withNewExistentials(Map.Immutable<ITermVar, ITermVar> newExistentials) {
            return new Success(newState, updatedVars, newConstraints, newCriticalEdges, newExistentials);
        }

        @Override
        public <T, E extends Throwable> T match(OnSuccess<T, E> onSuccess, Function1<Throwable, T> onFailure, Function1<Delay, T> onDelay) throws E {
            return onSuccess.apply(newState, updatedVars, newConstraints, newCriticalEdges, newExistentials);
        }
    }

    private static class Failure extends StepResult {

        private final @Nullable Throwable exception;

        private Failure(@Nullable Throwable exception) {
            this.exception = exception;
        }

        @Override
        public <T, E extends Throwable> T match(OnSuccess<T, E> onSuccess, Function1<Throwable, T> onFailure, Function1<Delay, T> onDelay) {
            return onFailure.apply(exception);
        }
    }

    private static class Delayed extends StepResult {

        private final Delay delay;

        private Delayed(Delay delay) {
            this.delay = delay;
        }

        @Override
        public <T, E extends Throwable> T match(OnSuccess<T, E> onSuccess, Function1<Throwable, T> onFailure, Function1<Delay, T> onDelay) throws E {
            return onDelay.apply(delay);
        }
    }

    public interface OnSuccess<R, E extends Throwable> {

        R apply(
                IState.Immutable newState,
                Set.Immutable<ITermVar> updatedVars,
                Collection<IConstraint> newConstraints,
                ICompleteness.Immutable newCriticalEdges,
                Map.Immutable<ITermVar, ITermVar> existentials
        ) throws E;
    }

    public interface OnSuccessAction  {
        void apply(
                IState.Immutable newState,
                Set.Immutable<ITermVar> updatedVars,
                Collection<IConstraint> newConstraints,
                ICompleteness.Immutable newCriticalEdges,
                Map.Immutable<ITermVar, ITermVar> existentials
        );
    }

}
