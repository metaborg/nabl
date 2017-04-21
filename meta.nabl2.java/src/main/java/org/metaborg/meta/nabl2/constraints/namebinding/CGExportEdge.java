package org.metaborg.meta.nabl2.constraints.namebinding;

import org.immutables.serial.Serial;
import org.immutables.value.Value;
import org.metaborg.meta.nabl2.constraints.IConstraint;
import org.metaborg.meta.nabl2.constraints.messages.IMessageContent;
import org.metaborg.meta.nabl2.constraints.messages.IMessageInfo;
import org.metaborg.meta.nabl2.constraints.messages.MessageContent;
import org.metaborg.meta.nabl2.scopegraph.terms.Label;
import org.metaborg.meta.nabl2.terms.ITerm;
import org.metaborg.meta.nabl2.terms.ITermVar;
import org.pcollections.PSet;

@Value.Immutable
@Serial.Version(value = 42L)
public abstract class CGExportEdge implements INamebindingConstraint {

    @Value.Parameter public abstract ITerm getDeclaration();

    @Value.Parameter public abstract Label getLabel();

    @Value.Parameter public abstract ITerm getScope();

    @Value.Parameter @Override public abstract IMessageInfo getMessageInfo();

    @Override public PSet<ITermVar> getVars() {
        return getDeclaration().getVars().plusAll(getScope().getVars());
    }
    
    @Override public <T> T match(Cases<T> cases) {
        return cases.caseAssoc(this);
    }

    @Override public <T> T match(IConstraint.Cases<T> cases) {
        return cases.caseNamebinding(this);
    }

    @Override public <T, E extends Throwable> T matchOrThrow(CheckedCases<T, E> cases) throws E {
        return cases.caseAssoc(this);
    }

    @Override public <T, E extends Throwable> T matchOrThrow(IConstraint.CheckedCases<T, E> cases) throws E {
        return cases.caseNamebinding(this);
    }

    @Override public IMessageContent pp() {
        return MessageContent.builder().append(getDeclaration()).append(" =" + getLabel().getName() + "=> ")
            .append(getScope()).build();
    }

    @Override public String toString() {
        return pp().toString();
    }

}