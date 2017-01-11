package org.metaborg.meta.nabl2.constraints.ast;

import org.immutables.value.Value;
import org.metaborg.meta.nabl2.constraints.IConstraint;
import org.metaborg.meta.nabl2.constraints.MessageInfo;
import org.metaborg.meta.nabl2.terms.ITerm;
import org.metaborg.meta.nabl2.terms.generic.TermIndex;
import org.metaborg.meta.nabl2.unification.IUnifier;

import com.google.common.base.Preconditions;

@Value.Immutable
public abstract class CAstProperty implements IAstConstraint {

    @Value.Parameter public abstract TermIndex getIndex();

    @Value.Parameter public abstract ITerm getKey();

    @Value.Parameter public abstract ITerm getValue();

    @Value.Parameter @Override public abstract MessageInfo getMessageInfo();

    @Override public IConstraint find(IUnifier unifier) {
        return ImmutableCAstProperty.of(getIndex(), getKey(), unifier.find(getValue()), getMessageInfo());
    }

    @Value.Check public void check() {
        Preconditions.checkArgument(getKey().isGround());
    }

    @Override public <T> T match(Cases<T> cases) {
        return cases.caseProperty(this);
    }

    @Override public <T> T match(IConstraint.Cases<T> cases) {
        return cases.caseAst(this);
    }

    @Override public <T, E extends Throwable> T matchOrThrow(CheckedCases<T,E> cases) throws E {
        return cases.caseProperty(this);
    }

    @Override public <T, E extends Throwable> T matchOrThrow(IConstraint.CheckedCases<T,E> cases) throws E {
        return cases.caseAst(this);
    }

    @Override public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(getIndex());
        sb.append(".");
        sb.append(getKey());
        sb.append(" := ");
        sb.append(getValue());
        return sb.toString();
    }

}