package mb.statix.solver.concurrent2.impl;

import mb.statix.actors.IActorRef;

public interface IBroker2ProjectProtocol<S, L, D> {

    void _start(Iterable<? extends IActorRef<? extends IUnit<S, L, D>>> units);

}