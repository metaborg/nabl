package mb.statix.concurrent.actors.deadlock;

import mb.statix.concurrent.actors.IActorRef;

public interface IDeadlockMonitor<T> {

    void waitFor(IActorRef<?> actor, T token);

    void granted(IActorRef<?> actor, T token);

    void suspended(Clock clock);

    void stopped(Clock clock);

}