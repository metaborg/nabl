package mb.scopegraph.pepm16.esop15;

import org.immutables.serial.Serial;
import org.immutables.value.Value;

import mb.scopegraph.pepm16.ILabel;
import mb.scopegraph.pepm16.IScope;

@Value.Immutable
@Serial.Version(value = 42L)
public abstract class ACriticalEdge {

    @Value.Parameter public abstract IScope scope();

    @Value.Parameter public abstract ILabel label();

    @Override public String toString() {
         final StringBuilder b = new StringBuilder("CriticalEdge{");
         b.append("scope=").append(scope());
         b.append(", ");
         b.append("label=").append(label());
         return b.append('}').toString();
    }

}