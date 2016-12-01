package org.metaborg.meta.nabl2.terms.generic;

import org.immutables.serial.Serial;
import org.immutables.value.Value;
import org.metaborg.meta.nabl2.terms.ITerm;
import org.metaborg.util.iterators.Iterables2;

@Value.Immutable
@Serial.Structural
public abstract class TermPair {

    public static Iterable<TermPair> EMPTY = Iterables2.empty();

    public abstract ITerm getFirst();

    public abstract ITerm getSecond();

    @Override public String toString() {
        return "<" + getFirst() + "," + getSecond() + ">";
    }

}