package mb.nabl2.constraints.poly;

import static mb.nabl2.terms.build.TermBuild.B;

import org.immutables.serial.Serial;
import org.immutables.value.Value;

import mb.nabl2.constraints.IConstraint;
import mb.nabl2.constraints.messages.IMessageContent;
import mb.nabl2.constraints.messages.IMessageInfo;
import mb.nabl2.constraints.messages.MessageContent;
import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.ITermVar;

@Value.Immutable
@Serial.Version(value = 42L)
public abstract class CInstantiate implements IPolyConstraint {

    @Value.Parameter public abstract ITerm getType();

    @Value.Parameter public abstract ITermVar getInstVars();

    @Value.Parameter public abstract ITerm getDeclaration();

    @Value.Parameter @Override public abstract IMessageInfo getMessageInfo();

    @Override public <T> T match(Cases<T> cases) {
        return cases.caseInstantiate(this);
    }

    @Override public <T> T match(IConstraint.Cases<T> cases) {
        return cases.casePoly(this);
    }

    @Override public <T, E extends Throwable> T matchOrThrow(CheckedCases<T, E> cases) throws E {
        return cases.caseInstantiate(this);
    }

    @Override public <T, E extends Throwable> T matchOrThrow(IConstraint.CheckedCases<T, E> cases) throws E {
        return cases.casePoly(this);
    }

    @Override public IMessageContent pp() {
        return MessageContent.builder().append(getType()).append(" instOf(").append(B.newList((ITerm) getInstVars()))
                .append(") ").append(getDeclaration()).build();
    }

    @Override public String toString() {
        return pp().toString();
    }

}