package mb.statix.concurrent.actors.deadlock;

import java.util.Objects;

import mb.nabl2.util.collections.MultiSet;
import mb.nabl2.util.collections.MultiSet.Immutable;
import mb.statix.concurrent.actors.IActorRef;

public class Clock<N> {

    private final MultiSet.Immutable<IActorRef<? extends N>> sent;
    private final MultiSet.Immutable<IActorRef<? extends N>> delivered;

    public Clock(Immutable<IActorRef<? extends N>> sent, Immutable<IActorRef<? extends N>> delivered) {
        this.sent = sent;
        this.delivered = delivered;
    }

    public MultiSet.Immutable<IActorRef<? extends N>> sent() {
        return sent;
    }

    public MultiSet.Immutable<IActorRef<? extends N>> delivered() {
        return delivered;
    }

    public Clock<N> delivered(IActorRef<? extends N> sender) {
        return new Clock<>(sent, delivered.add(sender));
    }

    public Clock<N> sent(IActorRef<? extends N> receiver) {
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


    @Override public String toString() {
        return "Clock[sent = " + sent + ", received = " + delivered + "]";
    }

    public static <N> Clock<N> of() {
        return new Clock<>(MultiSet.Immutable.of(), MultiSet.Immutable.of());
    }

}