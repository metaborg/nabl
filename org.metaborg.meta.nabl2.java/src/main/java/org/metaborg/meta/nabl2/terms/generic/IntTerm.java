package org.metaborg.meta.nabl2.terms.generic;

import org.immutables.serial.Serial;
import org.immutables.value.Value;
import org.metaborg.meta.nabl2.terms.IIntTerm;
import org.metaborg.meta.nabl2.terms.ITermFunction;

@Value.Immutable
@Serial.Structural
public abstract class IntTerm implements IIntTerm {

    public abstract int getValue();

    @Override public boolean isGround() {
        return true;
    }

    @Override public <T> T apply(ITermFunction<T> visitor) {
        return visitor.apply(this);
    }

}