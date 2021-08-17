package mb.statix.concurrent;

import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import org.immutables.value.Value;

import io.usethesource.capsule.Set;
import mb.nabl2.terms.ITermVar;
import mb.statix.constraints.messages.IMessage;
import mb.statix.solver.CriticalEdge;
import mb.statix.solver.Delay;
import mb.statix.solver.IConstraint;
import mb.statix.solver.IState;
import mb.statix.solver.completeness.ICompleteness;

@Value.Immutable
public abstract class ASolverState {

    @Value.Parameter public abstract IState.Immutable state();

    @Value.Parameter public abstract ICompleteness.Immutable completeness();

    @Value.Parameter public abstract java.util.Set<IConstraint> activeConstraints();

    @Value.Parameter public abstract Map<IConstraint, Delay> delayedConstraints();

    @Value.Parameter public abstract @Nullable Map<ITermVar, ITermVar> existentials();

    @Value.Parameter public abstract List<ITermVar> updatedVars();

    @Value.Parameter public abstract Map<IConstraint, IMessage> failed();

    @Value.Parameter public abstract Set.Immutable<CriticalEdge> delayedCloses();

}
