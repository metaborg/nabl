package mb.statix.scopegraph.reference;

import org.immutables.serial.Serial;
import org.immutables.value.Value;

import mb.nabl2.terms.ITerm;

@Value.Immutable
@Serial.Version(42L)
public abstract class ACriticalEdge {

    @Value.Parameter public abstract ITerm scope();

    @Value.Parameter public abstract ITerm label();

    @Override public String toString() {
        return scope() + "-" + label();
    }
}