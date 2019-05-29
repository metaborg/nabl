package mb.statix.solver.persistent;

import java.util.List;
import java.util.Map;

import org.immutables.serial.Serial;
import org.immutables.value.Value;

import com.google.common.collect.ImmutableSet;

import mb.nabl2.terms.ITermVar;
import mb.nabl2.terms.unification.IUnifier.Immutable;
import mb.statix.constraints.Constraints;
import mb.statix.scopegraph.reference.CriticalEdge;
import mb.statix.solver.Delay;
import mb.statix.solver.IConstraint;
import mb.statix.solver.ISolverResult;

@Value.Immutable
@Serial.Version(42L)
public abstract class ASolverResult implements ISolverResult {

    @Override @Value.Parameter public abstract State state();

    @Override @Value.Parameter public abstract List<IConstraint> errors();

    @Override @Value.Parameter public abstract Map<IConstraint, Delay> delays();

    @Override @Value.Parameter public abstract Map<ITermVar, ITermVar> existentials();
    
    @Override
    public Immutable unifier() {
        return state().unifier();
    }

    @Override
    public boolean hasErrors() {
        return !errors().isEmpty();
    }

    @Override
    public Delay delay() {
        ImmutableSet.Builder<ITermVar> vars = ImmutableSet.builder();
        ImmutableSet.Builder<CriticalEdge> scopes = ImmutableSet.builder();
        delays().values().stream().forEach(d -> {
            vars.addAll(d.vars());
            scopes.addAll(d.criticalEdges());
        });
        return new Delay(vars.build(), scopes.build());
    }

    @Override
    public IConstraint delayed() {
        return Constraints.conjoin(delays().keySet());
    }

}