package mb.statix.p_raffrayi.impl;

import mb.nabl2.util.collections.MultiSet;
import mb.nabl2.util.collections.MultiSet.Immutable;
import mb.statix.actors.IActorRef;

class Clock<S, L, D> {

    private final MultiSet.Immutable<IActorRef<? extends IUnit2UnitProtocol<S, L, D>>> sent;
    private final MultiSet.Immutable<IActorRef<? extends IUnit2UnitProtocol<S, L, D>>> received;

    public Clock(Immutable<IActorRef<? extends IUnit2UnitProtocol<S, L, D>>> sent,
            Immutable<IActorRef<? extends IUnit2UnitProtocol<S, L, D>>> received) {
        this.sent = sent;
        this.received = received;
    }

    public MultiSet.Immutable<IActorRef<? extends IUnit2UnitProtocol<S, L, D>>> sent() {
        return sent;
    }

    public MultiSet.Immutable<IActorRef<? extends IUnit2UnitProtocol<S, L, D>>> received() {
        return received;
    }

    public Clock<S, L, D> received(IActorRef<? extends IUnit2UnitProtocol<S, L, D>> sender) {
        return new Clock<>(sent, received.add(sender));
    }

    public Clock<S, L, D> sent(IActorRef<? extends IUnit2UnitProtocol<S, L, D>> receiver) {
        return new Clock<>(sent.add(receiver), received);
    }

    public static <S, L, D> Clock<S, L, D> of() {
        return new Clock<>(MultiSet.Immutable.of(), MultiSet.Immutable.of());
    }

}