package mb.statix.solver.persistent;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.immutables.value.Value;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import mb.nabl2.terms.ITermVar;
import mb.statix.solver.Delay;
import mb.statix.solver.IConstraint;

@Value.Immutable
abstract class AStepResult {

    @Value.Parameter public abstract State state();

    @Value.Parameter public abstract List<ITermVar> updatedVars();

    @Value.Parameter public abstract List<IConstraint> newConstraints();

    @Value.Parameter public abstract Map<Delay, IConstraint> delayedConstraints();

    @Value.Parameter public abstract Map<ITermVar, ITermVar> existentials();

    public static StepResult of(State newState) {
        return StepResult.of(newState, ImmutableList.of(), ImmutableList.of(), ImmutableMap.of(),
                ImmutableMap.of());
    }

    public static StepResult ofNew(State newState, Collection<IConstraint> newConstraints) {
        return StepResult.of(newState, ImmutableList.of(), newConstraints, ImmutableMap.of(), ImmutableMap.of());
    }

    public static StepResult ofDelay(State newState, Delay delay, IConstraint c) {
        return StepResult.of(newState, ImmutableList.of(), ImmutableList.of(), ImmutableMap.of(delay, c),
                ImmutableMap.of());
    }

}