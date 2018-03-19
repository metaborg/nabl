package mb.nabl2.constraints.base;

import org.immutables.serial.Serial;
import org.immutables.value.Value;

import mb.nabl2.constraints.IConstraint;
import mb.nabl2.constraints.messages.IMessageContent;
import mb.nabl2.constraints.messages.IMessageInfo;
import mb.nabl2.constraints.messages.MessageContent;
import mb.nabl2.constraints.messages.MessageContent.Builder;
import mb.nabl2.constraints.messages.MessageInfo;

@Value.Immutable
@Serial.Version(value = 42L)
public abstract class CConj implements IBaseConstraint {

    @Value.Parameter public abstract IConstraint getLeft();

    @Value.Parameter public abstract IConstraint getRight();

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
        // a == b, c <? d
        final Builder builder = MessageContent.builder();
        builder.append(getLeft().pp());
        builder.append(", ");
        builder.append(getRight().pp());
        return builder.build();
    }

    @Override public String toString() {
        return pp().toString();
    }

    public static IConstraint of(Iterable<IConstraint> constraints) {
        IConstraint conj = ImmutableCTrue.of(MessageInfo.empty());
        for(IConstraint constraint : constraints) {
            conj = ImmutableCConj.of(constraint, conj, MessageInfo.empty());
        }
        return conj;
    }

}