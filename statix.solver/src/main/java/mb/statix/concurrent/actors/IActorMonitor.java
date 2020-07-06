package mb.statix.concurrent.actors;

public interface IActorMonitor {

    default void started(IActor<?> self) {
    }

    default void suspended(IActor<?> self) {
    }

    default void resumed(IActor<?> self) {
    }

    default void stopped(IActor<?> self) {
    }

}