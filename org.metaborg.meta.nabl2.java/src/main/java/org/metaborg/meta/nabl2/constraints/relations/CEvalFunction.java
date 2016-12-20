package org.metaborg.meta.nabl2.constraints.relations;

import org.immutables.serial.Serial;
import org.immutables.value.Value;
import org.metaborg.meta.nabl2.constraints.IConstraint;
import org.metaborg.meta.nabl2.constraints.MessageInfo;
import org.metaborg.meta.nabl2.terms.ITerm;
import org.metaborg.meta.nabl2.unification.IUnifier;

@Value.Immutable
@Serial.Version(value = 42L)
public abstract class CEvalFunction implements IRelationConstraint {

    @Value.Parameter public abstract ITerm getResult();

    @Value.Parameter public abstract String getFunction();

    @Value.Parameter public abstract ITerm getTerm();

    @Value.Parameter @Override public abstract MessageInfo getMessageInfo();

    @Override public IConstraint find(IUnifier unifier) {
        return ImmutableCEvalFunction.of(unifier.find(getResult()), getFunction(), unifier.find(getTerm()),
                getMessageInfo());
    }

    @Override public <T> T match(Cases<T> cases) {
        return cases.caseEval(this);
    }

    @Override public <T> T match(IConstraint.Cases<T> cases) {
        return cases.caseRelation(this);
    }

    @Override public <T, E extends Throwable> T matchOrThrow(CheckedCases<T,E> cases) throws E {
        return cases.caseEval(this);
    }

    @Override public <T, E extends Throwable> T matchOrThrow(IConstraint.CheckedCases<T,E> cases) throws E {
        return cases.caseRelation(this);
    }

    @Override public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(getResult());
        sb.append(" is ");
        sb.append(getFunction());
        sb.append(" of ");
        sb.append(getTerm());
        return sb.toString();
    }

}