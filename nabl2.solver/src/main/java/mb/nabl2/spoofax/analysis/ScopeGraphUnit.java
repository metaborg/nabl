package mb.nabl2.spoofax.analysis;

import java.util.Set;

import org.immutables.serial.Serial;
import org.immutables.value.Value;

import com.google.common.collect.ImmutableSet;

import mb.nabl2.constraints.IConstraint;
import mb.nabl2.solver.Fresh;

@SuppressWarnings("serial")
@Value.Immutable
@Serial.Version(42L)
public abstract class ScopeGraphUnit implements IScopeGraphUnit {

    @Value.Default public String resource() {
        return "";
    }

    @Value.Default public Set<IConstraint> constraints() {
        return ImmutableSet.of();
    }

    @Value.Default public Fresh fresh() {
        return new Fresh();
    }

    @Value.Default public boolean isPrimary() {
        return true;
    }

}