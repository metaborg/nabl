package mb.statix.concurrent.actors;

import javax.annotation.Nullable;

import org.metaborg.util.functions.Function1;

import mb.statix.concurrent.actors.futures.ICompletable;
import mb.statix.concurrent.actors.futures.IFuture;

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
     * Get interface to send oneself messages.
     */
    T local();

    /**
     * Complete future as a message.
     */
    <U> void complete(ICompletable<U> completable, U value, Throwable ex);

    /**
     * Schedule future to be dispatched as a message on completion.
     */
    <U> IFuture<U> schedule(IFuture<U> future);

    /**
     * Get sender of the current message being handled.
     */
    @Nullable IActorRef<?> sender();

    /**
     * Get typed sender of the current message being handled.
     */
    @Nullable <U> IActorRef<U> sender(TypeTag<U> type);

    /**
     * Add a monitor.
     */
    void addMonitor(IActorMonitor monitor);

    /**
     * Stop the actor.
     */
    void stop();

    /**
     * Assertion that checks it is executed on the actors thread.
     */
    void assertOnActorThread();

    IActorStats stats();

}