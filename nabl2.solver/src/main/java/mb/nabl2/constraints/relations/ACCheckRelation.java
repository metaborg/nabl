package mb.nabl2.constraints.relations;

import org.immutables.serial.Serial;
import org.immutables.value.Value;

import mb.nabl2.constraints.IConstraint;
import mb.nabl2.constraints.messages.IMessageContent;
import mb.nabl2.constraints.messages.IMessageInfo;
import mb.nabl2.constraints.messages.MessageContent;
import mb.nabl2.relations.terms.RelationName;
import mb.nabl2.terms.ITerm;

@Value.Immutable
@Serial.Version(value = 42L)
public abstract class ACCheckRelation implements IRelationConstraint {

    @Value.Parameter public abstract ITerm getLeft();

    @Value.Parameter public abstract RelationName getRelation();

    @Value.Parameter public abstract ITerm getRight();

    @Value.Parameter @Override public abstract IMessageInfo getMessageInfo();

    @Override public <T> T match(Cases<T> cases) {
        return cases.caseCheck((CCheckRelation) this);
    }

    @Override public <T> T match(IConstraint.Cases<T> cases) {
        return cases.caseRelation(this);
    }

    @Override public <T, E extends Throwable> T matchOrThrow(CheckedCases<T, E> cases) throws E {
        return cases.caseCheck((CCheckRelation) this);
    }

    @Override public <T, E extends Throwable> T matchOrThrow(IConstraint.CheckedCases<T, E> cases) throws E {
        return cases.caseRelation(this);
    }

    @Override public IMessageContent pp() {
        return MessageContent.builder().append(getLeft()).append(" <" + getRelation() + "? ").append(getRight())
                .build();
    }

    @Override public String toString() {
        return pp().toString();
    }

}