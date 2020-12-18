package mb.statix.concurrent.actors;

import java.util.Set;

public interface IActorMonitor {

    /**
     * Actor is started.
     */
    default void started() {
    }

    /**
     * Actor sent a message to another actor.
     */
    default void sent(IActorRef<?> target, Set<String> tags) {
    }

    /**
     * Actor delivered a message from another actor.
     */
    default void delivered(IActorRef<?> source, Set<String> tags) {
    }

    /**
     * Actor suspended.
     */
    default void suspended() {
    }

    /**
     * Actor resumed.
     */
    default void resumed() {
    }

    /**
     * Actor stopped.
     */
    default void stopped() {
    }

    /**
     * Actor failed.
     */
    default void failed(Throwable ex) {
    }

}