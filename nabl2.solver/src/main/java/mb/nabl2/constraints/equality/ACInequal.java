package mb.nabl2.constraints.equality;

import org.immutables.serial.Serial;
import org.immutables.value.Value;

import mb.nabl2.constraints.IConstraint;
import mb.nabl2.constraints.messages.IMessageContent;
import mb.nabl2.constraints.messages.IMessageInfo;
import mb.nabl2.constraints.messages.MessageContent;
import mb.nabl2.terms.ITerm;

@Value.Immutable
@Serial.Version(value = 42L)
public abstract class ACInequal implements IEqualityConstraint {

    @Value.Parameter public abstract ITerm getLeft();

    @Value.Parameter public abstract ITerm getRight();

    @Value.Parameter @Override public abstract IMessageInfo getMessageInfo();

    @Override public <T> T match(Cases<T> cases) {
        return cases.caseInequal((CInequal) this);
    }

    @Override public <T> T match(IConstraint.Cases<T> cases) {
        return cases.caseEquality(this);
    }

    @Override public <T, E extends Throwable> T matchOrThrow(CheckedCases<T, E> cases) throws E {
        return cases.caseInequal((CInequal) this);
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