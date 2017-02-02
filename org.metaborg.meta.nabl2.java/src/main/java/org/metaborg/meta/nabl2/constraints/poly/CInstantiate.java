package org.metaborg.meta.nabl2.constraints.poly;

import org.immutables.serial.Serial;
import org.immutables.value.Value;
import org.metaborg.meta.nabl2.constraints.IConstraint;
import org.metaborg.meta.nabl2.constraints.MessageInfo;
import org.metaborg.meta.nabl2.terms.ITerm;
import org.metaborg.meta.nabl2.unification.IUnifier;

@Value.Immutable
@Serial.Version(value = 42L)
public abstract class CInstantiate implements IPolyConstraint {

    @Value.Parameter public abstract ITerm getType();

    @Value.Parameter public abstract ITerm getScheme();

    @Value.Parameter @Override public abstract MessageInfo getMessageInfo();

    @Override public IConstraint find(IUnifier unifier) {
        return ImmutableCInstantiate.of(unifier.find(getType()), unifier.find(getScheme()), getMessageInfo());
    }

    @Override public <T> T match(Cases<T> cases) {
        return cases.caseInstantiate(this);
    }

    @Override public <T> T match(IConstraint.Cases<T> cases) {
        return cases.casePoly(this);
    }

    @Override public <T, E extends Throwable> T matchOrThrow(CheckedCases<T,E> cases) throws E {
        return cases.caseInstantiate(this);
    }

    @Override public <T, E extends Throwable> T matchOrThrow(IConstraint.CheckedCases<T,E> cases) throws E {
        return cases.casePoly(this);
    }

    @Override public String toString() {
        return getType() + " instOf " + getScheme();
    }

}