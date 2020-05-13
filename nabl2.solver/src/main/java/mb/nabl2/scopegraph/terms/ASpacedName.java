package mb.nabl2.scopegraph.terms;

import org.immutables.serial.Serial;
import org.immutables.value.Value;

import mb.nabl2.scopegraph.INamespace;
import mb.nabl2.terms.ITerm;

@Value.Immutable
@Serial.Version(value = 42L)
public abstract class ASpacedName {

    @Value.Parameter public abstract INamespace getNamespace();

    @Value.Parameter public abstract ITerm getName();

    @Override public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(getNamespace().getName());
        sb.append('{');
        sb.append(getName());
        sb.append('}');
        return sb.toString();
    }

}