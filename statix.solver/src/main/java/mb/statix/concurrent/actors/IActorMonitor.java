package mb.statix.concurrent.actors;

import java.util.Set;

public interface IActorMonitor {

    default void started(IActor<?> self) {
    }

    default void sent(IActor<?> self, IActorRef<?> target, Set<String> tags) {
    }

    default void delivered(IActor<?> self, IActorRef<?> source, Set<String> tags) {
    }

    default void suspended(IActor<?> self) {
    }

    default void resumed(IActor<?> self) {
    }

    default void stopped(IActor<?> self) {
    }

}