package mb.statix.solver;

import java.util.Set;

import org.immutables.serial.Serial;
import org.immutables.value.Value;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableSet;

import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.ITermVar;

@Value.Immutable
@Serial.Version(value = 42L)
public abstract class AConfig {

    @Value.Parameter public abstract State state();

    @Value.Parameter public abstract Set<IConstraint> constraints();

    @Value.Default public Set<Delay> delays() {
        return ImmutableSet.of();
    }

    public Delay delay() {
        ImmutableSet.Builder<ITermVar> vars = ImmutableSet.builder();
        ImmutableMultimap.Builder<ITerm, ITerm> scopes = ImmutableMultimap.builder();
        delays().stream().forEach(d -> {
            vars.addAll(d.vars());
            scopes.putAll(d.scopes());
        });
        return new Delay(vars.build(), scopes.build());
    }

    @Value.Parameter public abstract Completeness completeness();

    @Value.Default public Set<IConstraint> errors() {
        return ImmutableSet.of();
    }

    public boolean hasErrors() {
        return !errors().isEmpty();
    }

}