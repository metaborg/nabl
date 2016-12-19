package org.metaborg.meta.nabl2.constraints.sets;

import java.util.Optional;

import org.immutables.serial.Serial;
import org.immutables.value.Value;
import org.metaborg.meta.nabl2.constraints.IConstraint;
import org.metaborg.meta.nabl2.constraints.MessageInfo;
import org.metaborg.meta.nabl2.terms.ITerm;

@Value.Immutable
@Serial.Version(value = 42L)
public abstract class CSubsetEq implements ISetConstraint {

    @Value.Parameter public abstract ITerm getLeft();

    @Value.Parameter public abstract ITerm getRight();

    @Value.Parameter public abstract Optional<String> getProjection();

    @Value.Parameter @Override public abstract MessageInfo getMessageInfo();

    @Override public <T> T match(Cases<T> cases) {
        return cases.caseSubsetEq(this);
    }

    @Override public <T> T match(IConstraint.Cases<T> cases) {
        return cases.caseSet(this);
    }

    @Override public <T, E extends Throwable> T matchOrThrow(CheckedCases<T,E> cases) throws E {
        return cases.caseSubsetEq(this);
    }

    @Override public <T, E extends Throwable> T matchOrThrow(IConstraint.CheckedCases<T,E> cases) throws E {
        return cases.caseSet(this);
    }

    @Override public String toString() {
        return getLeft() + " subseteq " + getRight();
    }

}