package org.metaborg.meta.nabl2.spoofax;

import org.immutables.serial.Serial;
import org.immutables.value.Value;
import org.metaborg.meta.nabl2.constraints.IConstraint;
import org.metaborg.meta.nabl2.terms.ITerm;

@Value.Immutable
@Serial.Version(value = 42L)
public interface UnitResult {

    @Value.Parameter ITerm getAST();

    @Value.Parameter Iterable<IConstraint> getConstraints();

}