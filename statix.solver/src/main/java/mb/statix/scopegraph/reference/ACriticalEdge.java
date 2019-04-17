package mb.statix.scopegraph.reference;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.immutables.serial.Serial;
import org.immutables.value.Value;

import mb.nabl2.terms.ITerm;
import mb.statix.taico.module.IModule;

@Value.Immutable
@Serial.Version(42L)
public abstract class ACriticalEdge {

    @Value.Parameter public abstract ITerm scope();

    @Value.Parameter public abstract ITerm label();
    
    @Value.Parameter @Nullable public abstract IModule cause();

    @Override public String toString() {
        return scope() + "-" + label();
    }

    public static CriticalEdge of(ITerm scope, ITerm label) {
        return CriticalEdge.of(scope, label, null);
    }
}