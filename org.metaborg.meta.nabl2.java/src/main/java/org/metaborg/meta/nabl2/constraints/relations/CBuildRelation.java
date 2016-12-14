package org.metaborg.meta.nabl2.constraints.relations;

import org.immutables.serial.Serial;
import org.immutables.value.Value;
import org.metaborg.meta.nabl2.constraints.IConstraint;
import org.metaborg.meta.nabl2.constraints.MessageInfo;
import org.metaborg.meta.nabl2.relations.terms.RelationName;
import org.metaborg.meta.nabl2.terms.ITerm;

@Value.Immutable
@Serial.Version(value = 42L)
public abstract class CBuildRelation implements IRelationConstraint {

    @Value.Parameter public abstract ITerm getLeft();

    @Value.Parameter public abstract RelationName getRelation();

    @Value.Parameter public abstract ITerm getRight();

    @Value.Parameter @Override public abstract MessageInfo getMessageInfo();
    
    @Override public <T> T match(Cases<T> cases) {
        return cases.caseBuild(this);
    }

    @Override public <T> T match(IConstraint.Cases<T> cases) {
        return cases.caseRelation(this);
    }

    @Override public <T, E extends Throwable> T matchOrThrow(CheckedCases<T,E> cases) throws E {
        return cases.caseBuild(this);
    }

    @Override public <T, E extends Throwable> T matchOrThrow(IConstraint.CheckedCases<T,E> cases) throws E {
        return cases.caseRelation(this);
    }

    @Override public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(getLeft());
        sb.append(" <");
        sb.append(getRelation());
        sb.append("! ");
        sb.append(getRight());
        return sb.toString();
    }

}