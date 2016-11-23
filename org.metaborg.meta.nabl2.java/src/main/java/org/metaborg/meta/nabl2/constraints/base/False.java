package org.metaborg.meta.nabl2.constraints.base;

import org.immutables.serial.Serial;
import org.immutables.value.Value;
import org.metaborg.meta.nabl2.constraints.IConstraintCases;

@Value.Immutable
@Serial.Structural
public abstract class False implements IBaseConstraint {

    @Override public <T> T match(IBaseConstraintCases<T> cases) {
        return cases.caseOf(this);
    }

    @Override public <T> T match(IConstraintCases<T> cases) {
        return cases.caseOf(this);
    }

}