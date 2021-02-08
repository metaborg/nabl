package mb.statix.solver.persistent;

import java.util.Map;
import java.util.Set;

import org.immutables.serial.Serial;
import org.immutables.value.Value;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

import mb.nabl2.terms.ITermVar;
import mb.nabl2.terms.unification.ud.PersistentUniDisunifier;
import mb.statix.constraints.Constraints;
import mb.statix.constraints.messages.IMessage;
import mb.statix.constraints.messages.MessageKind;
import mb.statix.solver.CriticalEdge;
import mb.statix.solver.Delay;
import mb.statix.solver.IConstraint;
import mb.statix.solver.IState;
import mb.statix.solver.completeness.Completeness;
import mb.statix.solver.completeness.ICompleteness;
import mb.statix.spec.Spec;

@Value.Immutable
@Serial.Version(42L)
public abstract class ASolverResult {

    @Value.Parameter public abstract Spec spec();

    @Value.Parameter public abstract IState.Immutable state();

    @Value.Parameter public abstract Map<IConstraint, IMessage> messages();

    @Value.Parameter public abstract Map<IConstraint, Delay> delays();

    @Value.Parameter public abstract Map<ITermVar, ITermVar> existentials();

    @Value.Parameter public abstract Set<ITermVar> updatedVars();

    @Value.Parameter public abstract Set<CriticalEdge> removedEdges();

    @Value.Parameter public abstract ICompleteness.Immutable completeness();

    @Value.Default public int totalSolved() {
        return 0;
    }

    @Value.Default public int totalCriticalEdges() {
        return 0;
    }

    public boolean hasErrors() {
        return messages().values().stream().anyMatch(m -> m.kind().equals(MessageKind.ERROR));
    }

    public Delay delay() {
        ImmutableSet.Builder<ITermVar> vars = ImmutableSet.builder();
        ImmutableSet.Builder<CriticalEdge> scopes = ImmutableSet.builder();
        delays().values().stream().forEach(d -> {
            vars.addAll(d.vars());
            scopes.addAll(d.criticalEdges());
        });
        return new Delay(vars.build(), scopes.build());
    }

    public IConstraint delayed() {
        return Constraints.conjoin(delays().keySet());
    }

    public static SolverResult of(Spec spec) {
        return SolverResult.of(spec, State.of(), ImmutableMap.of(), ImmutableMap.of(), ImmutableMap.of(),
                ImmutableSet.of(), ImmutableSet.of(), Completeness.Immutable.of());
    }

    public SolverResult combine(SolverResult other) {
        final SolverResult.Builder combined = SolverResult.builder().from(this);
        combined.state(state().add(other.state()));
        combined.putAllMessages(other.messages());
        combined.putAllDelays(other.delays());
        combined.putAllExistentials(other.existentials());
        combined.addAllUpdatedVars(other.updatedVars());
        combined.addAllRemovedEdges(other.removedEdges());
        combined.completeness(completeness().addAll(other.completeness(), PersistentUniDisunifier.Immutable.of()));
        combined.totalSolved(totalSolved() + other.totalSolved());
        combined.totalCriticalEdges(totalCriticalEdges() + other.totalCriticalEdges());
        return combined.build();
    }

}