package mb.statix.concurrent.actors.deadlock;

import mb.nabl2.util.collections.MultiSet;
import mb.nabl2.util.collections.MultiSet.Immutable;
import mb.statix.concurrent.actors.IActorRef;

public class Clock {

    private final MultiSet.Immutable<IActorRef<?>> sent;
    private final MultiSet.Immutable<IActorRef<?>> received;

    public Clock(Immutable<IActorRef<?>> sent, Immutable<IActorRef<?>> received) {
        this.sent = sent;
        this.received = received;
    }

    public MultiSet.Immutable<IActorRef<?>> sent() {
        return sent;
    }

    public MultiSet.Immutable<IActorRef<?>> received() {
        return received;
    }

    public Clock received(IActorRef<?> sender) {
        return new Clock(sent, received.add(sender));
    }

    public Clock sent(IActorRef<?> receiver) {
        return new Clock(sent.add(receiver), received);
    }

    public static Clock of() {
        return new Clock(MultiSet.Immutable.of(), MultiSet.Immutable.of());
    }

}