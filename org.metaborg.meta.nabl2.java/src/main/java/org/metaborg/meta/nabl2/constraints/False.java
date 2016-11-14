package org.metaborg.meta.nabl2.constraints;

import org.immutables.serial.Serial;
import org.immutables.value.Value;

@Value.Immutable
@Serial.Structural
public abstract class False implements IConstraint {

    @Override public <T> T accept(IConstraintVisitor<T> visitor) {
        return visitor.visit(this);
    }
    
}