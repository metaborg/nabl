package mb.statix.concurrent.actors.deadlock;

import java.util.Objects;
import java.util.Optional;

import mb.nabl2.util.collections.MultiSet;
import mb.nabl2.util.collections.MultiSet.Immutable;

public class Clock<N> {

    private final MultiSet.Immutable<N> sent;
    private final MultiSet.Immutable<N> delivered;

    public Clock(Immutable<N> sent, Immutable<N> delivered) {
        this.sent = sent;
        this.delivered = delivered;
    }

    public MultiSet.Immutable<N> sent() {
        return sent;
    }

    public MultiSet.Immutable<N> delivered() {
        return delivered;
    }

    public Clock<N> delivered(N sender) {
        return new Clock<>(sent, delivered.add(sender));
    }

    public Clock<N> sent(N receiver) {
        return new Clock<>(sent.add(receiver), delivered);
    }

    @Override public int hashCode() {
        return Objects.hash(sent, delivered);
    }

    @Override public boolean equals(Object obj) {
        if(this == obj)
            return true;
        if(obj == null)
            return false;
        if(getClass() != obj.getClass())
            return false;
        final Clock<?> other = (Clock<?>) obj;
        return sent.equals(other.sent) && delivered.equals(other.delivered);
    }

    public Optional<Integer> compareTo(Clock<N> other) {
        Integer ds = sent.compareTo(other.sent).orElse(null);
        Integer dr = delivered.compareTo(other.delivered).orElse(null);
        if(ds == null || dr == null) {
            return Optional.empty();
        } else if(ds < 0 & dr < 0) {
            return Optional.of(-1);
        } else if(ds > 0 && dr > 0) {
            return Optional.of(1);
        } else {
            return Optional.empty();
        }
    }

    @Override public String toString() {
        return "Clock[sent = " + sent + ", delivered = " + delivered + "]";
    }

    public static <N> Clock<N> of() {
        return new Clock<>(MultiSet.Immutable.of(), MultiSet.Immutable.of());
    }

}