package org.metaborg.meta.nabl2.constraints.controlflow;

import org.immutables.serial.Serial;
import org.immutables.value.Value;
import org.metaborg.meta.nabl2.constraints.IConstraint;
import org.metaborg.meta.nabl2.constraints.messages.IMessageContent;
import org.metaborg.meta.nabl2.constraints.messages.IMessageInfo;
import org.metaborg.meta.nabl2.constraints.messages.MessageContent;
import org.metaborg.meta.nabl2.terms.ITerm;

@Value.Immutable
@Serial.Version(value = 42L)
public abstract class CFDirectEdge<S extends ITerm> implements IControlFlowConstraint {

    @Value.Parameter public abstract S getSourceNode();

    @Value.Parameter public abstract ITerm getTargetNode();
    
    @Value.Parameter @Override public abstract IMessageInfo getMessageInfo();

    @Override public <T> T match(Cases<T> cases) {
        return cases.caseDirectEdge(this);
    }

    @Override public <T> T match(IConstraint.Cases<T> cases) {
        return cases.caseControlflow(this);
    }

    @Override public <T, E extends Throwable> T matchOrThrow(CheckedCases<T, E> cases) throws E {
        return cases.caseDirectEdge(this);
    }

    @Override public <T, E extends Throwable> T matchOrThrow(IConstraint.CheckedCases<T, E> cases) throws E {
        return cases.caseControlflow(this);
    }

    @Override public IMessageContent pp() {
        return MessageContent.builder().append(getSourceNode()).append(" ---> ")
            .append(getTargetNode()).build();
    }

    @Override public String toString() {
        return pp().toString();
    }

}