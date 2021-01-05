package mb.statix.concurrent.actors;

import javax.annotation.Nullable;

import org.metaborg.util.functions.Function1;

/**
 * Interface through which an actor implementation interacts with the underlying actor.
 */
public interface IActor<T> extends IActorRef<T> {

    /**
     * Start a sub actor.
     */
    <U> IActorRef<U> add(String id, TypeTag<U> type, Function1<IActor<U>, U> supplier);

    /**
     * Get the async interface to an actor to send messages.
     */
    <U> U async(IActorRef<U> receiver);

    /**
     * Get interface to send oneself messages.
     */
    T local();

    /**
     * Get sender of the current message being handled.
     */
    @Nullable IActorRef<?> sender();

    /**
     * Get typed sender of the current message being handled.
     */
    @Nullable <U> IActorRef<U> sender(TypeTag<U> type);

    /**
     * Assertion that checks it is executed on the actors thread.
     */
    void assertOnActorThread();

    IActorStats stats();

}