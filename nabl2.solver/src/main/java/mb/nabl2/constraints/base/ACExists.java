package mb.nabl2.constraints.base;

import org.immutables.serial.Serial;
import org.immutables.value.Value;

import io.usethesource.capsule.Set;
import mb.nabl2.constraints.IConstraint;
import mb.nabl2.constraints.messages.IMessageContent;
import mb.nabl2.constraints.messages.IMessageInfo;
import mb.nabl2.constraints.messages.MessageContent;
import mb.nabl2.constraints.messages.MessageContent.Builder;
import mb.nabl2.terms.ITermVar;

@Value.Immutable
@Serial.Version(value = 42L)
public abstract class ACExists implements IBaseConstraint {

    @Value.Parameter public abstract Set.Immutable<ITermVar> getEVars();

    @Value.Parameter public abstract IConstraint getConstraint();

    @Value.Parameter @Override public abstract IMessageInfo getMessageInfo();

    @Override public <T> T match(Cases<T> cases) {
        return cases.caseExists((CExists) this);
    }

    @Override public <T> T match(IConstraint.Cases<T> cases) {
        return cases.caseBase(this);
    }

    @Override public <T, E extends Throwable> T matchOrThrow(CheckedCases<T, E> cases) throws E {
        return cases.caseExists((CExists) this);
    }

    @Override public <T, E extends Throwable> T matchOrThrow(IConstraint.CheckedCases<T, E> cases) throws E {
        return cases.caseBase(this);
    }

    @Override public IMessageContent pp() {
        // exists {a b c}. c
        final Builder builder = MessageContent.builder();
        builder.append("exists {");
        boolean first = true;
        for(ITermVar var : getEVars()) {
            if(first) {
                first = false;
            } else {
                builder.append(" ");
            }
            builder.append(var);
        }
        builder.append(": ");
        builder.append(getConstraint().pp());
        builder.append("}");
        return builder.build();
    }

    @Override public String toString() {
        return pp().toString();
    }

}