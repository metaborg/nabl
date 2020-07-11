package mb.statix.concurrent.actors.deadlock;

import java.util.Objects;

import mb.nabl2.util.collections.MultiSet;
import mb.nabl2.util.collections.MultiSet.Immutable;

public class Clock<N> {

    private final MultiSet.Immutable<N> sent;
    private final MultiSet.Immutable<N> delivered;
    private final int internal;

    public Clock(Immutable<N> sent, Immutable<N> delivered, int internal) {
        this.sent = sent;
        this.delivered = delivered;
        this.internal = internal;
    }

    public MultiSet.Immutable<N> sent() {
        return sent;
    }

    public MultiSet.Immutable<N> delivered() {
        return delivered;
    }

    public Clock<N> delivered(N sender) {
        return new Clock<>(sent, delivered.add(sender), internal);
    }

    public Clock<N> sent(N receiver) {
        return new Clock<>(sent.add(receiver), delivered, internal);
    }

    public Clock<N> internal() {
        return new Clock<>(sent, delivered, internal + 1);
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


    @Override public String toString() {
        return "Clock[sent = " + sent + ", delivered = " + delivered + ", internal = " + internal + "]";
    }

    public static <N> Clock<N> of() {
        return new Clock<>(MultiSet.Immutable.of(), MultiSet.Immutable.of(), 0);
    }

}