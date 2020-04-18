package mb.statix.solver;

import org.immutables.serial.Serial;
import org.immutables.value.Value;

import mb.nabl2.terms.ITerm;
import mb.statix.scopegraph.reference.EdgeOrData;

@Value.Immutable
@Serial.Version(42L)
public abstract class ACriticalEdge {

    @Value.Parameter public abstract ITerm scope();

    @Value.Parameter public abstract EdgeOrData<ITerm> edgeOrData();

    @Override public String toString() {
        return scope() + "-" + edgeOrData();
    }

}