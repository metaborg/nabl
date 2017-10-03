package org.metaborg.meta.nabl2.constraints.controlflow;

import org.immutables.serial.Serial;
import org.immutables.value.Value;
import org.metaborg.meta.nabl2.constraints.IConstraint;
import org.metaborg.meta.nabl2.constraints.messages.IMessageContent;
import org.metaborg.meta.nabl2.constraints.messages.IMessageInfo;
import org.metaborg.meta.nabl2.constraints.messages.MessageContent;
import org.metaborg.meta.nabl2.terms.ITerm;

import com.google.common.base.Preconditions;

@Value.Immutable
@Serial.Version(value = 42L)
public abstract class CFDeclProperty implements IControlFlowConstraint {

    @Value.Parameter public abstract ITerm getDeclaration();

    @Value.Parameter public abstract ITerm getKey();

    @Value.Parameter public abstract ITerm getValue();

    @Value.Parameter public abstract int getPriority();

    @Value.Parameter @Override public abstract IMessageInfo getMessageInfo();

    @Value.Check public void check() {
        Preconditions.checkArgument(getKey().isGround());
    }

    @Override public <T> T match(Cases<T> cases) {
        return cases.caseProperty(this);
    }

    @Override public <T> T match(IConstraint.Cases<T> cases) {
        return cases.caseControlflow(this);
    }

    @Override public <T, E extends Throwable> T matchOrThrow(CheckedCases<T, E> cases) throws E {
        return cases.caseProperty(this);
    }

    @Override public <T, E extends Throwable> T matchOrThrow(IConstraint.CheckedCases<T, E> cases) throws E {
        return cases.caseControlflow(this);
    }

    @Override public IMessageContent pp() {
        return MessageContent.builder().append(getDeclaration()).append(".").append(getKey()).append(" := ")
            .append(getValue()).build();
    }

    @Override public String toString() {
        return pp().toString();
    }

}