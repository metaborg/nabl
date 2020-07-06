package mb.nabl2.constraints.sets;

import java.util.Optional;

import org.immutables.serial.Serial;
import org.immutables.value.Value;

import mb.nabl2.constraints.IConstraint;
import mb.nabl2.constraints.messages.IMessageContent;
import mb.nabl2.constraints.messages.IMessageInfo;
import mb.nabl2.constraints.messages.MessageContent;
import mb.nabl2.terms.ITerm;

@Value.Immutable
@Serial.Version(value = 42L)
public abstract class ACDistinct implements ISetConstraint {

    @Value.Parameter public abstract ITerm getSet();

    @Value.Parameter public abstract Optional<String> getProjection();

    @Value.Parameter @Override public abstract IMessageInfo getMessageInfo();

    @Override public <T> T match(Cases<T> cases) {
        return cases.caseDistinct((CDistinct) this);
    }

    @Override public <T> T match(IConstraint.Cases<T> cases) {
        return cases.caseSet(this);
    }

    @Override public <T, E extends Throwable> T matchOrThrow(CheckedCases<T, E> cases) throws E {
        return cases.caseDistinct((CDistinct) this);
    }

    @Override public <T, E extends Throwable> T matchOrThrow(IConstraint.CheckedCases<T, E> cases) throws E {
        return cases.caseSet(this);
    }

    @Override public IMessageContent pp() {
        return MessageContent.builder().append("distinct" + getProjection().map(p -> "/" + p + " ").orElse(" "))
                .append(getSet()).build();
    }

    @Override public String toString() {
        return pp().toString();
    }

}