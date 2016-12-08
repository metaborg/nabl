package org.metaborg.meta.nabl2.spoofax;

import org.immutables.serial.Serial;
import org.immutables.value.Value;
import org.metaborg.meta.nabl2.constraints.IConstraint;

@Value.Immutable
@Serial.Structural
public interface InitialResult {

    @Value.Parameter Iterable<IConstraint> getConstraints();

    @Value.Parameter Args getArgs();

}