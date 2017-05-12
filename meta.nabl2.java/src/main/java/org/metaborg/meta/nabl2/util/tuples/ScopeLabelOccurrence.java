package org.metaborg.meta.nabl2.util.tuples;

import org.immutables.serial.Serial;
import org.immutables.value.Value;
import org.metaborg.meta.nabl2.scopegraph.ILabel;
import org.metaborg.meta.nabl2.scopegraph.IOccurrence;
import org.metaborg.meta.nabl2.scopegraph.IScope;

@Value.Immutable
@Serial.Version(value = 42L)
public abstract class ScopeLabelOccurrence<S extends IScope, L extends ILabel, O extends IOccurrence> {

    @Value.Parameter public abstract S scope();

    @Value.Parameter public abstract L label();

    @Value.Parameter public abstract O occurrence();

}