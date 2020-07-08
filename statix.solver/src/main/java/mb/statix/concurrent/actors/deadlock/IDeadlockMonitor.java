package mb.statix.concurrent.actors.deadlock;

import mb.statix.concurrent.actors.IActorRef;

public interface IDeadlockMonitor<N, S, T> {

    void waitFor(IActorRef<? extends N> actor, T token);

    void granted(IActorRef<? extends N> actor, T token);

    void suspended(S state, Clock<N> clock);

    void stopped(Clock<N> clock);

}