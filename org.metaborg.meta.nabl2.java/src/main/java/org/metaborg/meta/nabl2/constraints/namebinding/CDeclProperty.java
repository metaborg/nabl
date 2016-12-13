package org.metaborg.meta.nabl2.constraints.namebinding;

import org.immutables.value.Value;
import org.metaborg.meta.nabl2.constraints.IConstraint;
import org.metaborg.meta.nabl2.terms.ITerm;

import com.google.common.base.Preconditions;

@Value.Immutable
public abstract class CDeclProperty implements INamebindingConstraint {

    @Value.Parameter public abstract ITerm getDeclaration();

    @Value.Parameter public abstract ITerm getKey();

    @Value.Parameter public abstract ITerm getValue();

    @Value.Check public void check() {
        Preconditions.checkArgument(getKey().isGround());
    }

    @Override public <T> T match(Cases<T> cases) {
        return cases.caseProperty(this);
    }

    @Override public <T> T match(IConstraint.Cases<T> cases) {
        return cases.caseNamebinding(this);
    }

    @Override public <T, E extends Throwable> T matchOrThrow(CheckedCases<T,E> cases) throws E {
        return cases.caseProperty(this);
    }

    @Override public <T, E extends Throwable> T matchOrThrow(IConstraint.CheckedCases<T,E> cases) throws E {
        return cases.caseNamebinding(this);
    }

    @Override public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(getDeclaration());
        sb.append(".");
        sb.append(getKey());
        sb.append(" := ");
        sb.append(getValue());
        return sb.toString();
    }

}