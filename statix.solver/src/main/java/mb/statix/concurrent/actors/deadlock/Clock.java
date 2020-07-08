package mb.statix.concurrent.actors.deadlock;

import mb.nabl2.util.collections.MultiSet;
import mb.nabl2.util.collections.MultiSet.Immutable;
import mb.statix.concurrent.actors.IActorRef;

public class Clock<N> {

    private final MultiSet.Immutable<IActorRef<? extends N>> sent;
    private final MultiSet.Immutable<IActorRef<? extends N>> received;

    public Clock(Immutable<IActorRef<? extends N>> sent, Immutable<IActorRef<? extends N>> received) {
        this.sent = sent;
        this.received = received;
    }

    public MultiSet.Immutable<IActorRef<? extends N>> sent() {
        return sent;
    }

    public MultiSet.Immutable<IActorRef<? extends N>> received() {
        return received;
    }

    public Clock<N> received(IActorRef<? extends N> sender) {
        return new Clock<>(sent, received.add(sender));
    }

    public Clock<N> sent(IActorRef<? extends N> receiver) {
        return new Clock<>(sent.add(receiver), received);
    }

    public static <N> Clock<N> of() {
        return new Clock<>(MultiSet.Immutable.of(), MultiSet.Immutable.of());
    }

}