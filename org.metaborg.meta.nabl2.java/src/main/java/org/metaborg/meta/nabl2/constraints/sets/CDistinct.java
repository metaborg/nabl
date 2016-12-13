package org.metaborg.meta.nabl2.constraints.sets;

import org.immutables.serial.Serial;
import org.immutables.value.Value;
import org.metaborg.meta.nabl2.constraints.IConstraint;
import org.metaborg.meta.nabl2.terms.ITerm;

@Value.Immutable
@Serial.Version(value = 42L)
public abstract class CDistinct implements ISetConstraint {

    @Value.Parameter public abstract ITerm getSet();

    @Override public <T> T match(Cases<T> cases) {
        return cases.caseDistinct(this);
    }

    @Override public <T> T match(IConstraint.Cases<T> cases) {
        return cases.caseSet(this);
    }

    @Override public <T, E extends Throwable> T matchOrThrow(CheckedCases<T,E> cases) throws E {
        return cases.caseDistinct(this);
    }

    @Override public <T, E extends Throwable> T matchOrThrow(IConstraint.CheckedCases<T,E> cases) throws E {
        return cases.caseSet(this);
    }

    @Override public String toString() {
        return "distinct " + getSet();
    }

}