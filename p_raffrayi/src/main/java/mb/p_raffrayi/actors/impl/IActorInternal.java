package mb.p_raffrayi.actors.impl;

import java.lang.reflect.Method;

import org.metaborg.util.functions.Function1;
import org.metaborg.util.future.ICompletable;

import mb.p_raffrayi.actors.IActor;
import mb.p_raffrayi.actors.IActorRef;

/**
 * Interface through which actors interact with each other.
 */
public interface IActorInternal<T> extends IActorRef<T> {

    void _start(IActorInternal<?> sender, Function1<IActor<T>, ? extends T> supplier);

    /**
     * Return a dynamic async interface to the actor, for use in other actors. The interface is dynamic in that it
     * relies on {@link ActorThreadLocals#current} to get the invoking actor.
     * 
     * The implementation can use a single cached object to be used by all actors.
     */
    T _invokeDynamic();

    /**
     * Return a static async interface to the actor, for use by the actor system. The interface does not rely on
     * {@link ActorThreadLocals#current}.
     */
    T _invokeStatic(IActorInternal<?> sender);

    void _return(IActorInternal<?> sender, Method method, @SuppressWarnings("rawtypes") ICompletable result,
            Object value, Throwable ex);

    /**
     * Tell the actor to stop.
     * 
     * @param sender
     *            TODO
     */
    void _stop(IActorInternal<?> sender, Throwable ex);

    /**
     * A child of this actor has stopped.
     * 
     * @param sender
     *            TODO
     */
    void _childStopped(IActorInternal<?> sender, Throwable ex);

}