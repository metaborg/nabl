package org.metaborg.meta.nabl2.constraints.namebinding;

import org.immutables.serial.Serial;
import org.immutables.value.Value;
import org.metaborg.meta.nabl2.constraints.IConstraint;
import org.metaborg.meta.nabl2.constraints.MessageInfo;
import org.metaborg.meta.nabl2.terms.ITerm;
import org.metaborg.meta.nabl2.unification.IUnifier;

import com.google.common.base.Preconditions;

@Value.Immutable
@Serial.Version(value = 42L)
public abstract class CDeclProperty implements INamebindingConstraint {

    @Value.Parameter
    public abstract ITerm getDeclaration();

    @Value.Parameter
    public abstract ITerm getKey();

    @Value.Parameter
    public abstract ITerm getValue();

    @Value.Parameter
    public abstract int getPriority();

    @Value.Parameter
    @Override
    public abstract MessageInfo getMessageInfo();

    @Override
    public IConstraint find(IUnifier unifier) {
        return ImmutableCDeclProperty.of(unifier.find(getDeclaration()), getKey(), unifier.find(getValue()),
                getPriority(), getMessageInfo());
    }

    @Value.Check
    public void check() {
        Preconditions.checkArgument(getKey().isGround());
    }

    @Override
    public <T> T match(Cases<T> cases) {
        return cases.caseProperty(this);
    }

    @Override
    public <T> T match(IConstraint.Cases<T> cases) {
        return cases.caseNamebinding(this);
    }

    @Override
    public <T, E extends Throwable> T matchOrThrow(CheckedCases<T, E> cases) throws E {
        return cases.caseProperty(this);
    }

    @Override
    public <T, E extends Throwable> T matchOrThrow(IConstraint.CheckedCases<T, E> cases) throws E {
        return cases.caseNamebinding(this);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(getDeclaration());
        sb.append(".");
        sb.append(getKey());
        sb.append(" := ");
        sb.append(getValue());
        return sb.toString();
    }

}