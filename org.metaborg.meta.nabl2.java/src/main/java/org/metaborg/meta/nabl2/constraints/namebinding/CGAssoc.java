package org.metaborg.meta.nabl2.constraints.namebinding;

import org.immutables.serial.Serial;
import org.immutables.value.Value;
import org.metaborg.meta.nabl2.constraints.IConstraint;
import org.metaborg.meta.nabl2.terms.ITerm;

@Value.Immutable
@Serial.Structural
public abstract class CGAssoc implements INamebindingConstraint {

    @Value.Parameter public abstract ITerm getDeclaration();

    @Value.Parameter public abstract ITerm getLabel();

    @Value.Parameter public abstract ITerm getScope();

    @Override public <T> T match(Cases<T> cases) {
        return cases.caseAssoc(this);
    }

    @Override public <T> T match(IConstraint.Cases<T> cases) {
        return cases.caseNamebinding(this);
    }

    @Override public <T, E extends Throwable> T matchOrThrow(CheckedCases<T,E> cases) throws E {
        return cases.caseAssoc(this);
    }

    @Override public <T, E extends Throwable> T matchOrThrow(IConstraint.CheckedCases<T,E> cases) throws E {
        return cases.caseNamebinding(this);
    }

    @Override public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(getDeclaration());
        sb.append("=");
        sb.append(getLabel());
        sb.append("=>");
        sb.append(getScope());
        return sb.toString();
    }

}