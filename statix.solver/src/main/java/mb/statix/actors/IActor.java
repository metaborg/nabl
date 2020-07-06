package mb.statix.actors;

import javax.annotation.Nullable;

import org.metaborg.util.functions.Function1;

public interface IActor<T> extends IActorRef<T> {

    /**
     * Start a sub actor.
     */
    <U> IActor<U> add(String id, TypeTag<U> type, Function1<IActor<U>, U> supplier);

    /**
     * Get the async interface to an actor to send messages.
     */
    <U> U async(IActorRef<U> receiver);

    /**
     * Get sender of the current message being handled.
     */
    @Nullable IActorRef<?> sender();

    /**
     * Add a monitor.
     */
    void addMonitor(IActorMonitor monitor);

    /**
     * Stop the actor.
     */
    void stop();

}