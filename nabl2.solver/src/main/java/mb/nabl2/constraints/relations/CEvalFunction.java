package mb.nabl2.constraints.relations;

import org.immutables.serial.Serial;
import org.immutables.value.Value;

import mb.nabl2.constraints.IConstraint;
import mb.nabl2.constraints.messages.IMessageContent;
import mb.nabl2.constraints.messages.IMessageInfo;
import mb.nabl2.constraints.messages.MessageContent;
import mb.nabl2.relations.terms.FunctionName;
import mb.nabl2.terms.ITerm;

@Value.Immutable
@Serial.Version(value = 42L)
public abstract class CEvalFunction implements IRelationConstraint {

    @Value.Parameter public abstract ITerm getResult();

    @Value.Parameter public abstract FunctionName getFunction();

    @Value.Parameter public abstract ITerm getTerm();

    @Value.Parameter @Override public abstract IMessageInfo getMessageInfo();

    @Override public <T> T match(Cases<T> cases) {
        return cases.caseEval(this);
    }

    @Override public <T> T match(IConstraint.Cases<T> cases) {
        return cases.caseRelation(this);
    }

    @Override public <T, E extends Throwable> T matchOrThrow(CheckedCases<T, E> cases) throws E {
        return cases.caseEval(this);
    }

    @Override public <T, E extends Throwable> T matchOrThrow(IConstraint.CheckedCases<T, E> cases) throws E {
        return cases.caseRelation(this);
    }

    @Override public IMessageContent pp() {
        return MessageContent.builder().append(getResult()).append(" is " + getFunction() + " of ").append(getTerm())
                .build();
    }

    @Override public String toString() {
        return pp().toString();
    }

}