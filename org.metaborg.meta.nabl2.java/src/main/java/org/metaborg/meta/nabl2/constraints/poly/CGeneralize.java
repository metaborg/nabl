package org.metaborg.meta.nabl2.constraints.poly;

import org.immutables.serial.Serial;
import org.immutables.value.Value;
import org.metaborg.meta.nabl2.constraints.IConstraint;
import org.metaborg.meta.nabl2.constraints.MessageInfo;
import org.metaborg.meta.nabl2.terms.ITerm;
import org.metaborg.meta.nabl2.unification.IUnifier;

@Value.Immutable
@Serial.Version(value = 42L)
public abstract class CGeneralize implements IPolyConstraint {

    @Value.Parameter public abstract ITerm getScheme();

    @Value.Parameter public abstract ITerm getType();

    @Value.Parameter @Override public abstract MessageInfo getMessageInfo();

    @Override public IConstraint find(IUnifier unifier) {
        return ImmutableCGeneralize.of(unifier.find(getScheme()), unifier.find(getType()), getMessageInfo());
    }

    @Override public <T> T match(Cases<T> cases) {
        return cases.caseGeneralize(this);
    }

    @Override public <T> T match(IConstraint.Cases<T> cases) {
        return cases.casePoly(this);
    }

    @Override public <T, E extends Throwable> T matchOrThrow(CheckedCases<T,E> cases) throws E {
        return cases.caseGeneralize(this);
    }

    @Override public <T, E extends Throwable> T matchOrThrow(IConstraint.CheckedCases<T,E> cases) throws E {
        return cases.casePoly(this);
    }

    @Override public String toString() {
        return getScheme() + " genOf " + getType();
    }

}