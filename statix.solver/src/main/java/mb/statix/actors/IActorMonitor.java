package mb.statix.actors;

public interface IActorMonitor {

    default void started(IActorRef<?> actor) {
    }

    default void suspended(IActorRef<?> actor) {
    }

    default void resumed(IActorRef<?> actor) {
    }

    default void stopped(IActorRef<?> actor) {
    }

}