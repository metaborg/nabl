package mb.statix.concurrent.actors.deadlock;

import mb.nabl2.util.collections.MultiSet;
import mb.nabl2.util.collections.MultiSet.Immutable;

public class Clock<N> {

    private final MultiSet.Immutable<N> sent;
    private final MultiSet.Immutable<N> received;

    public Clock(Immutable<N> sent, Immutable<N> received) {
        this.sent = sent;
        this.received = received;
    }

    public MultiSet.Immutable<N> sent() {
        return sent;
    }

    public MultiSet.Immutable<N> received() {
        return received;
    }

    public Clock<N> received(N sender) {
        return new Clock<>(sent, received.add(sender));
    }

    public Clock<N> sent(N receiver) {
        return new Clock<>(sent.add(receiver), received);
    }

    public static <N> Clock<N> of() {
        return new Clock<>(MultiSet.Immutable.of(), MultiSet.Immutable.of());
    }

}