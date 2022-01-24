package mb.statix.concurrent;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.annotation.Nullable;

import org.immutables.serial.Serial;
import org.immutables.value.Value;

import io.usethesource.capsule.Set;

import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.ITermVar;
import mb.p_raffrayi.ITypeChecker;
import mb.statix.constraints.messages.IMessage;
import mb.statix.scopegraph.Scope;
import mb.statix.solver.CriticalEdge;
import mb.statix.solver.IConstraint;
import mb.statix.solver.IState;
import mb.statix.solver.completeness.ICompleteness;

@Value.Immutable
@Serial.Version(42)
public abstract class ASolverState implements ITypeChecker.IState<Scope, ITerm, ITerm> {

    @Value.Parameter public abstract IState.Immutable state();

    @Value.Parameter public abstract ICompleteness.Immutable completeness();

    @Value.Parameter public abstract java.util.Set<IConstraint> constraints();

    @Value.Parameter public abstract @Nullable Map<ITermVar, ITermVar> existentials();

    @Value.Parameter public abstract List<ITermVar> updatedVars();

    @Value.Parameter public abstract Map<IConstraint, IMessage> failed();

    @Value.Parameter public abstract Set.Immutable<CriticalEdge> delayedCloses();

    @Override public Optional<ITerm> tryGetExternalDatum(ITerm datum) {
        if(datum.isGround()) {
            return Optional.of(datum);
        }
        ITerm term = state().unifier().findRecursive(datum);
        return term.isGround() ? Optional.of(term) : Optional.empty();
    }

}
