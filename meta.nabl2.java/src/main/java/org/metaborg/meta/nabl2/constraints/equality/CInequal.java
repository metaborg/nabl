package org.metaborg.meta.nabl2.constraints.equality;

import org.immutables.serial.Serial;
import org.immutables.value.Value;
import org.metaborg.meta.nabl2.constraints.IConstraint;
import org.metaborg.meta.nabl2.constraints.messages.IMessageContent;
import org.metaborg.meta.nabl2.constraints.messages.IMessageInfo;
import org.metaborg.meta.nabl2.constraints.messages.MessageContent;
import org.metaborg.meta.nabl2.terms.ITerm;
import org.metaborg.meta.nabl2.terms.ITermVar;
import org.pcollections.PSet;

@Value.Immutable
@Serial.Version(value = 42L)
public abstract class CInequal implements IEqualityConstraint {

    @Value.Parameter public abstract ITerm getLeft();

    @Value.Parameter public abstract ITerm getRight();

    @Value.Parameter @Override public abstract IMessageInfo getMessageInfo();

    @Override public PSet<ITermVar> getVars() {
        return getLeft().getVars().plusAll(getRight().getVars());
    }
    
    @Override public <T> T match(Cases<T> cases) {
        return cases.caseInequal(this);
    }

    @Override public <T> T match(IConstraint.Cases<T> cases) {
        return cases.caseEquality(this);
    }

    @Override public <T, E extends Throwable> T matchOrThrow(CheckedCases<T, E> cases) throws E {
        return cases.caseInequal(this);
    }

    @Override public <T, E extends Throwable> T matchOrThrow(IConstraint.CheckedCases<T, E> cases) throws E {
        return cases.caseEquality(this);
    }

    @Override public IMessageContent pp() {
        return MessageContent.builder().append(getLeft()).append(" != ").append(getRight()).build();
    }

    @Override public String toString() {
        return pp().toString();
    }

}