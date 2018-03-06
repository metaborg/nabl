package mb.nabl2.terms.build;

import org.immutables.serial.Serial;
import org.immutables.value.Value;
import org.metaborg.util.iterators.Iterables2;

import mb.nabl2.terms.ITerm;

@Value.Immutable
@Serial.Version(value = 42L)
public abstract class TermPair {

    public static final Iterable<TermPair> EMPTY = Iterables2.empty();

    @Value.Parameter abstract ITerm getFirst();

    @Value.Parameter public abstract ITerm getSecond();

    @Override public String toString() {
        return "<" + getFirst() + "," + getSecond() + ">";
    }

}
