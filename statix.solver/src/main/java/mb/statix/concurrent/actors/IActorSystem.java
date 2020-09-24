package mb.statix.concurrent.actors;

import org.metaborg.util.functions.Function1;


public interface IActorSystem {

    <T> IActorRef<T> add(String id, TypeTag<T> type, Function1<IActor<T>, T> supplier);

    /**
     * Get async interface to actor.
     */
    <T> T async(IActorRef<T> receiver);

    void addMonitor(IActorRef<?> actor, IActorRef<? extends IActorMonitor> monitor);

    /**
     * Start actor system.
     */
    void start();

    /**
     * Stop actor system, actors may complete current work.
     */
    void stop();

    /**
     * Stop actor system, interrupt actors current work.
     */
    void cancel();

}