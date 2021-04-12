package mb.statix.concurrent.actors.deadlock;

import org.metaborg.util.collection.MultiSet;

import mb.statix.concurrent.actors.IActorRef;

public interface IDeadlockMonitor<N> {

    void waitFor(IActorRef<? extends N> actor);

    void granted(IActorRef<? extends N> actor);

    default void suspended(Clock<IActorRef<? extends N>> clock) {
        suspended(clock, MultiSet.Immutable.of(), MultiSet.Immutable.of());
    }

    void suspended(Clock<IActorRef<? extends N>> clock, MultiSet.Immutable<IActorRef<? extends N>> waitFors,
            MultiSet.Immutable<IActorRef<? extends N>> grants);

}