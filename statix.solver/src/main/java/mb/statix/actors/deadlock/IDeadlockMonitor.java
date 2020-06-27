package mb.statix.actors.deadlock;

import mb.statix.actors.IActorMonitor;
import mb.statix.actors.IActorRef;

public interface IDeadlockMonitor<T> extends IActorMonitor {

    void waitFor(IActorRef<?> source, T token, IActorRef<?> target);

    void granted(IActorRef<?> source, T token, IActorRef<?> target);

}