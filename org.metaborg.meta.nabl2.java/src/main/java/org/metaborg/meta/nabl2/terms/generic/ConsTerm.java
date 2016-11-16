package org.metaborg.meta.nabl2.terms.generic;

import org.immutables.serial.Serial;
import org.immutables.value.Value;
import org.metaborg.meta.nabl2.terms.IConsTerm;
import org.metaborg.meta.nabl2.terms.IListTerm;
import org.metaborg.meta.nabl2.terms.ITerm;
import org.metaborg.meta.nabl2.terms.ITermFunction;

@Value.Immutable
@Serial.Structural
abstract class ConsTerm implements IConsTerm {

    public abstract ITerm getHead();

    public abstract IListTerm getTail();

    @Value.Lazy @Override public boolean isGround() {
        return getHead().isGround() && getTail().isGround();
    }

    @Value.Lazy @Override public int size() {
        return 1 + getTail().size();
    }

    @Override public <T> T apply(ITermFunction<T> visitor) {
        return visitor.apply(this);
    }
    
}