package mb.nabl2.constraints.scopegraph;

import org.immutables.serial.Serial;
import org.immutables.value.Value;

import mb.nabl2.constraints.IConstraint;
import mb.nabl2.constraints.messages.IMessageContent;
import mb.nabl2.constraints.messages.IMessageInfo;
import mb.nabl2.constraints.messages.MessageContent;
import mb.nabl2.terms.ITerm;
import mb.scopegraph.pepm16.terms.Label;

@Value.Immutable
@Serial.Version(value = 42L)
public abstract class ACGDirectEdge implements IScopeGraphConstraint {

    @Value.Parameter public abstract ITerm getSourceScope();

    @Value.Parameter public abstract Label getLabel();

    @Value.Parameter public abstract ITerm getTargetScope();

    @Value.Parameter @Override public abstract IMessageInfo getMessageInfo();

    @Override public <T> T match(Cases<T> cases) {
        return cases.caseDirectEdge((CGDirectEdge) this);
    }

    @Override public <T> T match(IConstraint.Cases<T> cases) {
        return cases.caseScopeGraph(this);
    }

    @Override public <T, E extends Throwable> T matchOrThrow(CheckedCases<T, E> cases) throws E {
        return cases.caseDirectEdge((CGDirectEdge) this);
    }

    @Override public <T, E extends Throwable> T matchOrThrow(IConstraint.CheckedCases<T, E> cases) throws E {
        return cases.caseScopeGraph(this);
    }

    @Override public IMessageContent pp() {
        return MessageContent.builder().append(getSourceScope()).append(" -" + getLabel().getName() + "-> ")
                .append(getTargetScope()).build();
    }

    @Override public String toString() {
        return pp().toString();
    }

}