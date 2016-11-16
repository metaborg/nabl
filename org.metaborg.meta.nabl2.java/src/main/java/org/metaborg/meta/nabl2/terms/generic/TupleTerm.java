package org.metaborg.meta.nabl2.terms.generic;

import org.immutables.serial.Serial;
import org.immutables.value.Value;
import org.metaborg.meta.nabl2.terms.ITerm;
import org.metaborg.meta.nabl2.terms.ITermFunction;
import org.metaborg.meta.nabl2.terms.ITupleTerm;

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

    @Override public <T> T apply(ITermFunction<T> visitor) {
        return visitor.apply(this);
    }

}