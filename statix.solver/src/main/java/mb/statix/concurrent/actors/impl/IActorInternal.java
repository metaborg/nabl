package mb.statix.concurrent.actors.impl;

import org.metaborg.util.functions.Function1;

import mb.statix.concurrent.actors.IActor;
import mb.statix.concurrent.actors.IActorRef;

/**
 * Interface through which actors interact with each other.
 */
public interface IActorInternal<T> extends IActorRef<T> {

    void _start(Function1<IActor<T>, ? extends T> supplier);

    /**
     * Return a dynamic async interface to the actor, for use in other actors. The interface is dynamic in that it
     * relies on {@link ActorThreadLocals#current} to get the invoking actor.
     * 
     * The implementation can use a single cached object to be used by all actors.
     */
    T _dynamicAsync();

    /**
     * Return a static async interface to the actor, for use by the actor system. The interface does not rely on
     * {@link ActorThreadLocals#current}.
     */
    T _staticAsync(IActorInternal<?> system);

    /**
     * Tell the actor to stop.
     */
    void _stop(Throwable ex);

    /**
     * A child of this actor has stopped.
     */
    void _childStopped(Throwable ex);

}