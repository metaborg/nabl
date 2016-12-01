package org.metaborg.meta.nabl2.terms.generic;

import org.immutables.serial.Serial;
import org.immutables.value.Value;
import org.metaborg.meta.nabl2.terms.IAnnotation;
import org.metaborg.meta.nabl2.terms.IApplTerm;
import org.metaborg.meta.nabl2.terms.ITerm;

import com.google.common.collect.ImmutableClassToInstanceMap;
import com.google.common.collect.Iterables;

@Value.Immutable
@Serial.Structural
abstract class ApplTerm implements IApplTerm {

    @Override public abstract String getOp();

    @Override public abstract Iterable<ITerm> getArgs();

    @Value.Auxiliary @Override public abstract ImmutableClassToInstanceMap<IAnnotation> getAnnotations();

    @Value.Lazy @Override public int getArity() {
        return Iterables.size(getArgs());
    }

    @Value.Lazy @Override public boolean isGround() {
        boolean ground = true;
        for (ITerm arg : getArgs()) {
            ground &= arg.isGround();
        }
        return ground;
    }

    @Override public <T> T match(Cases<T> cases) {
        return cases.caseAppl(this);
    }

    @Override public <T, E extends Throwable> T matchThrows(CheckedCases<T,E> cases) throws E {
        return cases.caseAppl(this);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(getOp());
        sb.append("(");
        boolean first = true;
        for(ITerm arg : getArgs()) {
            if (first) {
                first = false;
            } else {
                sb.append(",");
            }
            sb.append(arg.toString());
        }
        sb.append(")");
        return sb.toString();
    }

}