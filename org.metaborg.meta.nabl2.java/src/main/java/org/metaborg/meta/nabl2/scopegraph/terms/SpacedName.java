package org.metaborg.meta.nabl2.scopegraph.terms;

import org.immutables.serial.Serial;
import org.immutables.value.Value;
import org.metaborg.meta.nabl2.scopegraph.INamespace;
import org.metaborg.meta.nabl2.scopegraph.IOccurrence;
import org.metaborg.meta.nabl2.terms.ITerm;

@Value.Immutable
@Serial.Version(value = 42L)
public abstract class SpacedName {

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

    public static SpacedName of(IOccurrence occurrence) {
        return ImmutableSpacedName.of(occurrence.getNamespace(), occurrence.getName());
    }

}