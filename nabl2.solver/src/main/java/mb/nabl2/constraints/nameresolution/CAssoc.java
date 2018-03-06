package mb.nabl2.constraints.nameresolution;

import org.immutables.serial.Serial;
import org.immutables.value.Value;

import mb.nabl2.constraints.IConstraint;
import mb.nabl2.constraints.messages.IMessageContent;
import mb.nabl2.constraints.messages.IMessageInfo;
import mb.nabl2.constraints.messages.MessageContent;
import mb.nabl2.scopegraph.terms.Label;
import mb.nabl2.terms.ITerm;

@Value.Immutable
@Serial.Version(value = 42L)
public abstract class CAssoc implements INameResolutionConstraint {

    @Value.Parameter public abstract ITerm getDeclaration();

    @Value.Parameter public abstract Label getLabel();

    @Value.Parameter public abstract ITerm getScope();

    @Value.Parameter @Override public abstract IMessageInfo getMessageInfo();

    @Override public <T> T match(Cases<T> cases) {
        return cases.caseAssoc(this);
    }

    @Override public <T> T match(IConstraint.Cases<T> cases) {
        return cases.caseNameResolution(this);
    }

    @Override public <T, E extends Throwable> T matchOrThrow(CheckedCases<T, E> cases) throws E {
        return cases.caseAssoc(this);
    }

    @Override public <T, E extends Throwable> T matchOrThrow(IConstraint.CheckedCases<T, E> cases) throws E {
        return cases.caseNameResolution(this);
    }

    @Override public IMessageContent pp() {
        return MessageContent.builder().append(getDeclaration()).append(" ?=" + getLabel().getName() + "=> ")
                .append(getScope()).build();
    }

    @Override public String toString() {
        return pp().toString();
    }

}