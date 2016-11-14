package org.metaborg.meta.nabl2.constraints;

import java.util.List;

import org.immutables.serial.Serial;
import org.immutables.value.Value;

@Value.Immutable
@Serial.Structural
public abstract class Conj implements IConstraint {

    public abstract List<IConstraint> getConstraints();

    @Override public <T> T accept(IConstraintVisitor<T> visitor) {
        return visitor.visit(this);
    }
    
}