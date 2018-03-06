package org.metaborg.meta.nabl2.constraints.controlflow;

import java.util.List;

import org.immutables.serial.Serial;
import org.immutables.value.Value;
import org.metaborg.meta.nabl2.constraints.IConstraint;
import org.metaborg.meta.nabl2.constraints.messages.IMessageContent;
import org.metaborg.meta.nabl2.constraints.messages.IMessageInfo;
import org.metaborg.meta.nabl2.constraints.messages.MessageContent;
import org.metaborg.meta.nabl2.controlflow.terms.CFGNode;
import org.metaborg.meta.nabl2.terms.ITerm;

@Value.Immutable
@Serial.Version(value = 42L)
public abstract class CTFAppl implements IControlFlowConstraint {

    @Value.Parameter public abstract CFGNode getCFGNode();

    @Value.Parameter public abstract String getPropertyName();
    
    @Value.Parameter public abstract int getOffset();

    @Value.Parameter public abstract List<ITerm> getArguments();

    @Value.Parameter @Override public abstract IMessageInfo getMessageInfo();

    @Override public <T> T match(Cases<T> cases) {
        return cases.caseTFAppl(this);
    }

    @Override public <T> T match(IConstraint.Cases<T> cases) {
        return cases.caseControlflow(this);
    }

    @Override public <T, E extends Throwable> T matchOrThrow(CheckedCases<T, E> cases) throws E {
        return cases.caseTFAppl(this);
    }

    @Override public <T, E extends Throwable> T matchOrThrow(IConstraint.CheckedCases<T, E> cases) throws E {
        return cases.caseControlflow(this);
    }

    @Override public IMessageContent pp() {
        MessageContent.Builder b = MessageContent.builder()
                .append(getCFGNode())
                .append(".")
                .append(getPropertyName())
                .append(":")
                .append(Integer.toString(getOffset()))
                .append(" := [");
        getArguments().forEach(t -> b.append(t));
        b.append("]");
        return b.build();
    }

    @Override public String toString() {
        return pp().toString();
    }

}