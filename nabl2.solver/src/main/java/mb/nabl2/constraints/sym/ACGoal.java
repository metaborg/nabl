package mb.nabl2.constraints.sym;

import org.immutables.serial.Serial;
import org.immutables.value.Value;

import mb.nabl2.constraints.IConstraint;
import mb.nabl2.constraints.messages.IMessageContent;
import mb.nabl2.constraints.messages.IMessageInfo;
import mb.nabl2.constraints.messages.MessageContent;
import mb.nabl2.terms.ITerm;

@Value.Immutable
@Serial.Version(value = 42L)
public abstract class ACGoal implements ISymbolicConstraint {

    @Value.Parameter public abstract ITerm getGoal();

    @Value.Parameter @Override public abstract IMessageInfo getMessageInfo();

    @Override public <T> T match(Cases<T> cases) {
        return cases.caseGoal((CGoal) this);
    }

    @Override public <T> T match(IConstraint.Cases<T> cases) {
        return cases.caseSym(this);
    }

    @Override public <T, E extends Throwable> T matchOrThrow(CheckedCases<T, E> cases) throws E {
        return cases.caseGoal((CGoal) this);
    }

    @Override public <T, E extends Throwable> T matchOrThrow(IConstraint.CheckedCases<T, E> cases) throws E {
        return cases.caseSym(this);
    }

    @Override public IMessageContent pp() {
        return MessageContent.builder().append("?- ").append(getGoal()).build();
    }

    @Override public String toString() {
        return pp().toString();
    }

}