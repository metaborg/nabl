package mb.statix.p_raffrayi.impl;

import mb.statix.actors.IActorRef;

public interface IBroker2ProjectProtocol<S, L, D> {

    void _start(Iterable<? extends IActorRef<? extends IUnit<S, L, D>>> units);

}