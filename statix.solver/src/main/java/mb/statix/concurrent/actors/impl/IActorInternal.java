package mb.statix.concurrent.actors.impl;

import org.metaborg.util.functions.Function1;

import mb.statix.concurrent.actors.IActor;
import mb.statix.concurrent.actors.IActorRef;

/**
 * Interface through which actors interact with each other.
 */
public interface IActorInternal<T> extends IActorRef<T> {

    void start(Function1<IActor<T>, ? extends T> supplier);

    T async();
    
    void stop(Throwable ex);

}