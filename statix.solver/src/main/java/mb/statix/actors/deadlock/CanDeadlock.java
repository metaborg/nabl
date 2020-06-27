package mb.statix.actors.deadlock;

import io.usethesource.capsule.SetMultimap;
import mb.statix.actors.IActorRef;

public interface CanDeadlock<T> {

    void deadlocked(SetMultimap<IActorRef<?>, T> waitFors);

}