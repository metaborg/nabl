package org.metaborg.meta.nabl2.terms.generic;

import org.immutables.serial.Serial;
import org.immutables.value.Value;
import org.metaborg.meta.nabl2.terms.INilTerm;
import org.metaborg.meta.nabl2.terms.ITermFunction;

@Value.Immutable
@Serial.Structural
abstract class NilTerm implements INilTerm {

    @Override public boolean isGround() {
        return true;
    }

    @Override public int size() {
        return 0;
    }

    @Override public <T> T apply(ITermFunction<T> visitor) {
        return visitor.apply(this);
    }
    
}
