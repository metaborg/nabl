package org.metaborg.meta.nabl2.constraints.base;

import java.util.List;

import org.immutables.serial.Serial;
import org.immutables.value.Value;
import org.metaborg.meta.nabl2.constraints.IConstraint;
import org.metaborg.meta.nabl2.constraints.messages.IMessageContent;
import org.metaborg.meta.nabl2.constraints.messages.IMessageInfo;
import org.metaborg.meta.nabl2.constraints.messages.MessageContent;
import org.metaborg.meta.nabl2.constraints.messages.MessageContent.Builder;

@Value.Immutable
@Serial.Version(value = 42L)
public abstract class CConj implements IBaseConstraint {

    @Value.Parameter public abstract List<IConstraint> getConstraints();

    @Value.Parameter @Override public abstract IMessageInfo getMessageInfo();

    @Override public <T> T match(Cases<T> cases) {
        return cases.caseConj(this);
    }

    @Override public <T> T match(IConstraint.Cases<T> cases) {
        return cases.caseBase(this);
    }

    @Override public <T, E extends Throwable> T matchOrThrow(CheckedCases<T, E> cases) throws E {
        return cases.caseConj(this);
    }

    @Override public <T, E extends Throwable> T matchOrThrow(IConstraint.CheckedCases<T, E> cases) throws E {
        return cases.caseBase(this);
    }

    @Override public IMessageContent pp() {
        // (a == b, c <? d)
        final Builder builder = MessageContent.builder();
        builder.append("(");
        boolean first = true;
        for(IConstraint c : getConstraints()) {
            if(first) {
                first = false;
            } else {
                builder.append(", ");
            }
            builder.append(c.pp());
        }
        builder.append(")");
        return builder.build();
    }

    @Override public String toString() {
        return pp().toString();
    }

}