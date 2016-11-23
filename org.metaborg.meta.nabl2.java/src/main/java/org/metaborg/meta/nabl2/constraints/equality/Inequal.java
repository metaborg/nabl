package org.metaborg.meta.nabl2.constraints.equality;

import org.immutables.serial.Serial;
import org.immutables.value.Value;
import org.metaborg.meta.nabl2.constraints.IConstraint;
import org.metaborg.meta.nabl2.constraints.IConstraintCases;

@Value.Immutable
@Serial.Structural
public abstract class Inequal implements IEqualityConstraint {

    public abstract IConstraint getLeft();

    public abstract IConstraint getRight();

    @Override public <T> T match(IEqualityConstraintCases<T> cases) {
        return cases.apply(this);
    }

    @Override public <T> T match(IConstraintCases<T> cases) {
        return cases.caseOf(this);
    }

}