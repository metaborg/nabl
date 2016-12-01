package org.metaborg.meta.nabl2.terms.generic;

import org.immutables.serial.Serial;
import org.immutables.value.Value;
import org.metaborg.meta.nabl2.terms.IAnnotation;
import org.metaborg.meta.nabl2.terms.ITerm;
import org.metaborg.meta.nabl2.terms.ITupleTerm;

import com.google.common.collect.ImmutableClassToInstanceMap;
import com.google.common.collect.Iterables;

@Value.Immutable
@Serial.Structural
abstract class TupleTerm implements ITupleTerm {

    @Override public abstract Iterable<ITerm> getArgs();

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

    @Value.Auxiliary @Override public abstract ImmutableClassToInstanceMap<IAnnotation> getAnnotations();

    @Override public <T> T match(Cases<T> cases) {
        return cases.caseTuple(this);
    }

    @Override public <T, E extends Throwable> T matchThrows(CheckedCases<T,E> cases) throws E {
        return cases.caseTuple(this);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
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