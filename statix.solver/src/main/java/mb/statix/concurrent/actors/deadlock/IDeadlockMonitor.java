package mb.statix.concurrent.actors.deadlock;

import mb.nabl2.util.collections.MultiSetMap;
import mb.statix.concurrent.actors.IActorRef;

public interface IDeadlockMonitor<N, S, T> {

    void waitFor(IActorRef<? extends N> actor, T token);

    void granted(IActorRef<? extends N> actor, T token);

    default void suspended(S state, Clock<IActorRef<? extends N>> clock) {
        suspended(state, clock, MultiSetMap.Immutable.of(), MultiSetMap.Immutable.of());
    }

    void suspended(S state, Clock<IActorRef<? extends N>> clock,
            MultiSetMap.Immutable<IActorRef<? extends N>, T> waitFors,
            MultiSetMap.Immutable<IActorRef<? extends N>, T> grants);

    default void stopped(Clock<IActorRef<? extends N>> clock) {
        stopped(clock, MultiSetMap.Immutable.of(), MultiSetMap.Immutable.of());
    }

    void stopped(Clock<IActorRef<? extends N>> clock, MultiSetMap.Immutable<IActorRef<? extends N>, T> waitFors,
            MultiSetMap.Immutable<IActorRef<? extends N>, T> grants);

}