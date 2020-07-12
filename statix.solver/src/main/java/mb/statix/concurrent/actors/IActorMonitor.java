package mb.statix.concurrent.actors;

import java.util.Set;

public interface IActorMonitor {

    /**
     * Actor is started.
     */
    default void started(IActor<?> self) {
    }

    /**
     * Actor sent a message to another actor.
     */
    default void sent(IActor<?> self, IActorRef<?> target, Set<String> tags) {
    }

    /**
     * Actor delivered a message from another actor.
     */
    default void delivered(IActor<?> self, IActorRef<?> source, Set<String> tags) {
    }

    /**
     * Actor suspended.
     */
    default void suspended(IActor<?> self) {
    }

    /**
     * Actor resumed.
     */
    default void resumed(IActor<?> self) {
    }

    /**
     * Actor stopped.
     */
    default void stopped(IActor<?> self) {
    }

    /**
     * Actor failed.
     */
    default void failed(IActor<?> self, Throwable ex) {
    }

}