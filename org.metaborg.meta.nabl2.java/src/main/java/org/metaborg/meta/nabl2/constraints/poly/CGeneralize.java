package org.metaborg.meta.nabl2.constraints.poly;

import org.immutables.serial.Serial;
import org.immutables.value.Value;
import org.metaborg.meta.nabl2.constraints.IConstraint;
import org.metaborg.meta.nabl2.constraints.messages.IMessageContent;
import org.metaborg.meta.nabl2.constraints.messages.IMessageInfo;
import org.metaborg.meta.nabl2.constraints.messages.MessageContent;
import org.metaborg.meta.nabl2.terms.ITerm;
import org.metaborg.meta.nabl2.terms.ITermVar;
import org.metaborg.meta.nabl2.terms.generic.TB;
import org.pcollections.PSet;

@Value.Immutable
@Serial.Version(value = 42L)
public abstract class CGeneralize implements IPolyConstraint {

    @Value.Parameter public abstract ITerm getDeclaration();

    @Value.Parameter public abstract ITermVar getGenVars();

    @Value.Parameter public abstract ITerm getType();

    @Value.Parameter @Override public abstract IMessageInfo getMessageInfo();

    @Override public PSet<ITermVar> getVars() {
        return getDeclaration().getVars().plusAll(getType().getVars());
    }

    @Override public <T> T match(Cases<T> cases) {
        return cases.caseGeneralize(this);
    }

    @Override public <T> T match(IConstraint.Cases<T> cases) {
        return cases.casePoly(this);
    }

    @Override public <T, E extends Throwable> T matchOrThrow(CheckedCases<T, E> cases) throws E {
        return cases.caseGeneralize(this);
    }

    @Override public <T, E extends Throwable> T matchOrThrow(IConstraint.CheckedCases<T, E> cases) throws E {
        return cases.casePoly(this);
    }

    @Override public IMessageContent pp() {
        return MessageContent.builder().append(getDeclaration()).append(" genOf(").append(TB.newList((ITerm) getGenVars()))
                .append(") ").append(getType()).build();
    }

    @Override public String toString() {
        return pp().toString();
    }

}