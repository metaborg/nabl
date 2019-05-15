package mb.statix.solver.persistent;

import java.util.List;
import java.util.Map;

import org.immutables.serial.Serial;
import org.immutables.value.Value;

import com.google.common.collect.ImmutableSet;

import mb.nabl2.terms.ITermVar;
import mb.statix.constraints.Constraints;
import mb.statix.scopegraph.reference.CriticalEdge;
import mb.statix.solver.Delay;
import mb.statix.solver.IConstraint;

@Value.Immutable
@Serial.Version(42L)
public abstract class ASolverResult {

    @Value.Parameter public abstract State state();

    @Value.Parameter public abstract List<IConstraint> errors();

    @Value.Parameter public abstract Map<IConstraint, Delay> delays();

    @Value.Parameter public abstract Map<ITermVar, ITermVar> existentials();

    public boolean hasErrors() {
        return !errors().isEmpty();
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

}