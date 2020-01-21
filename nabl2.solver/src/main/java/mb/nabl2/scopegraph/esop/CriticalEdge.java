package mb.nabl2.scopegraph.esop;

import org.immutables.serial.Serial;
import org.immutables.value.Value;

import mb.nabl2.scopegraph.ILabel;
import mb.nabl2.scopegraph.IScope;

@Value.Immutable
@Serial.Version(value = 42L)
public abstract class CriticalEdge {

    @Value.Parameter public abstract IScope scope();

    @Value.Parameter public abstract ILabel label();

}