package mb.statix.solver.persistent;

import java.util.Collection;
import java.util.List;

import org.immutables.value.Value;
import org.metaborg.util.collection.CapsuleUtil;
import org.metaborg.util.collection.ImList;

import io.usethesource.capsule.Map;
import mb.nabl2.terms.ITermVar;
import mb.statix.solver.Delay;
import mb.statix.solver.IConstraint;
import mb.statix.solver.IState;

@Value.Immutable
abstract class AStepResult {

    @Value.Parameter public abstract IState.Immutable state();

    @Value.Parameter public abstract ImList.Immutable<ITermVar> updatedVars();

    @Value.Parameter public abstract ImList.Immutable<IConstraint> newConstraints();

    @Value.Parameter public abstract Map.Immutable<Delay, IConstraint> delayedConstraints();

    @Value.Parameter public abstract Map.Immutable<ITermVar, ITermVar> existentials();

    public static StepResult of(IState.Immutable newState) {
        return StepResult.of(newState, ImList.Immutable.of(), ImList.Immutable.of(), CapsuleUtil.immutableMap(), Map.Immutable.of());
    }

    public static StepResult ofNew(IState.Immutable newState, ImList.Immutable<IConstraint> newConstraints) {
        return StepResult.of(newState, ImList.Immutable.of(), newConstraints, CapsuleUtil.immutableMap(), Map.Immutable.of());
    }

    public static StepResult ofDelay(IState.Immutable newState, Delay delay, IConstraint c) {
        return StepResult.of(newState, ImList.Immutable.of(), ImList.Immutable.of(), Map.Immutable.of(delay, c),
                Map.Immutable.of());
    }

}