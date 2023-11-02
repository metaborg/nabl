package mb.p_raffrayi.actors;

import jakarta.annotation.Nullable;

import org.metaborg.util.functions.Function1;
import org.metaborg.util.future.ICompletable;
import org.metaborg.util.future.IFuture;

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
     * Schedule handling of the given future as if it was sent as a message. Handlers attached to the returned future
     * are not executed immediately, but scheduled later, at some time after processing the current message has
     * finished.
     */
    <U> IFuture<U> schedule(IFuture<U> future);

    /**
     * Schedule completion of the given completable as if it was sent as a message.
     */
    <U> void complete(ICompletable<U> completable, U result, Throwable ex);

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