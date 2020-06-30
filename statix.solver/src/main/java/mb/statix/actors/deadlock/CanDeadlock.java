package mb.statix.actors.deadlock;

import io.usethesource.capsule.SetMultimap;
import mb.statix.actors.IActorRef;

public interface CanDeadlock<T> {

    /**
     * Deadlock was detected, involving the given wait-fors on this actor.
     */
    void deadlocked(SetMultimap<IActorRef<?>, T> waitFors);

}