package mb.nabl2.constraints.base;

import java.util.Set;

import org.immutables.serial.Serial;
import org.immutables.value.Value;

import mb.nabl2.constraints.IConstraint;
import mb.nabl2.constraints.messages.IMessageContent;
import mb.nabl2.constraints.messages.IMessageInfo;
import mb.nabl2.constraints.messages.MessageContent;
import mb.nabl2.constraints.messages.MessageContent.Builder;
import mb.nabl2.terms.ITerm;

@Value.Immutable
@Serial.Version(value = 42L)
public abstract class ACNew implements IBaseConstraint {

    @Value.Parameter public abstract Set<ITerm> getNVars();

    @Value.Parameter @Override public abstract IMessageInfo getMessageInfo();

    @Override public <T> T match(Cases<T> cases) {
        return cases.caseNew((CNew) this);
    }

    @Override public <T> T match(IConstraint.Cases<T> cases) {
        return cases.caseBase(this);
    }

    @Override public <T, E extends Throwable> T matchOrThrow(CheckedCases<T, E> cases) throws E {
        return cases.caseNew((CNew) this);
    }

    @Override public <T, E extends Throwable> T matchOrThrow(IConstraint.CheckedCases<T, E> cases) throws E {
        return cases.caseBase(this);
    }

    @Override public IMessageContent pp() {
        final Builder builder = MessageContent.builder();
        builder.append("new ");
        boolean first = true;
        for(ITerm var : getNVars()) {
            if(first) {
                first = false;
            } else {
                builder.append(" ");
            }
            builder.append(var);
        }
        return builder.build();
    }

    @Override public String toString() {
        return pp().toString();
    }

}