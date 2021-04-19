package mb.p_raffrayi.actors;

public interface IActorMonitor {

    /**
     * Actor is started.
     */
    default void started() {
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
     * Actor stopped or failed.
     */
    default void stopped(@SuppressWarnings("unused") Throwable ex) {
    }

}