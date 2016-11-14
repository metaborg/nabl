package org.metaborg.meta.nabl2.terms.generic;

import org.immutables.serial.Serial;
import org.immutables.value.Value;
import org.metaborg.meta.nabl2.terms.ITerm;
import org.metaborg.meta.nabl2.terms.ITermFunction;
import org.metaborg.meta.nabl2.terms.ITermVar;

@Value.Immutable
@Serial.Structural
public abstract class TermVar implements ITerm, ITermVar {
    
    public abstract String getName();

    public boolean isGround() {
        return false;
    }
    
    @Override public <T> T apply(ITermFunction<T> visitor) {
        return visitor.apply(this);
    }
    
}