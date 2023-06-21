package mb.nabl2.constraints.ast;

import org.immutables.serial.Serial;
import org.immutables.value.Value;

import mb.nabl2.constraints.IConstraint;
import mb.nabl2.constraints.messages.IMessageContent;
import mb.nabl2.constraints.messages.IMessageInfo;
import mb.nabl2.constraints.messages.MessageContent;
import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.stratego.TermIndex;

@Value.Immutable
@Serial.Version(value = 42L)
public abstract class ACAstProperty implements IAstConstraint {

    @Value.Parameter public abstract TermIndex getIndex();

    @Value.Parameter public abstract ITerm getKey();

    @Value.Parameter public abstract ITerm getValue();

    @Value.Parameter @Override public abstract IMessageInfo getMessageInfo();

    @Value.Check public void check() {
        if(!getKey().isGround()) {
            throw new IllegalArgumentException("Key is not ground");
        }
    }

    @Override public <T> T match(Cases<T> cases) {
        return cases.caseProperty((CAstProperty) this);
    }

    @Override public <T> T match(IConstraint.Cases<T> cases) {
        return cases.caseAst(this);
    }

    @Override public <T, E extends Throwable> T matchOrThrow(CheckedCases<T, E> cases) throws E {
        return cases.caseProperty((CAstProperty) this);
    }

    @Override public <T, E extends Throwable> T matchOrThrow(IConstraint.CheckedCases<T, E> cases) throws E {
        return cases.caseAst(this);
    }

    @Override public IMessageContent pp() {
        return MessageContent.builder().append(getIndex()).append(".").append(getKey()).append(" := ")
                .append(getValue()).build();
    }

    @Override public String toString() {
        return pp().toString();
    }

}