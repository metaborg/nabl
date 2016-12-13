package org.metaborg.meta.nabl2.constraints.base;

import org.immutables.serial.Serial;
import org.immutables.value.Value;
import org.metaborg.meta.nabl2.constraints.IConstraint;

@Value.Immutable
@Serial.Structural
public abstract class CTrue implements IBaseConstraint {

    @Override public <T> T match(Cases<T> cases) {
        return cases.caseTrue(this);
    }

    @Override public <T> T match(IConstraint.Cases<T> cases) {
        return cases.caseBase(this);
    }

    @Override public <T, E extends Throwable> T matchOrThrow(CheckedCases<T,E> cases) throws E {
        return cases.caseTrue(this);
    }

    @Override public <T, E extends Throwable> T matchOrThrow(IConstraint.CheckedCases<T,E> cases) throws E {
        return cases.caseBase(this);
    }

    @Override public String toString() {
        return "true";
    }

}