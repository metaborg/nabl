package org.metaborg.meta.nabl2.spoofax;

import java.util.Optional;

import org.immutables.serial.Serial;
import org.immutables.value.Value;
import org.metaborg.meta.nabl2.terms.ITerm;

@Value.Immutable
@Serial.Version(value = 42L)
public interface Args {

    @Value.Parameter Iterable<ITerm> getParams();

    @Value.Parameter Optional<ITerm> getType();

}