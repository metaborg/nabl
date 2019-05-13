package mb.statix.solver.persistent;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.immutables.value.Value;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import mb.nabl2.terms.ITermVar;
import mb.statix.solver.IConstraint;

@Value.Immutable
abstract class AConstraintResult {

    @Value.Parameter public abstract State state();

    @Value.Parameter public abstract List<IConstraint> constraints();

    @Value.Parameter public abstract List<ITermVar> vars();

    @Value.Default public Map<ITermVar, ITermVar> existentials() {
        return ImmutableMap.of();
    }

    public static ConstraintResult of(State state) {
        return ConstraintResult.of(state, ImmutableList.of(), ImmutableList.of());
    }

    public static ConstraintResult ofConstraints(State state, IConstraint... constraints) {
        return ofConstraints(state, Arrays.asList(constraints));
    }

    public static ConstraintResult ofConstraints(State state, Iterable<? extends IConstraint> constraints) {
        return ConstraintResult.of(state, ImmutableList.copyOf(constraints), ImmutableList.of());
    }

}