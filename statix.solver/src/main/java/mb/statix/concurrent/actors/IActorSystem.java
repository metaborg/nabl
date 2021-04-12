package mb.statix.concurrent.actors;

import org.metaborg.util.functions.Function1;
import org.metaborg.util.future.IFuture;
import org.metaborg.util.unit.Unit;


public interface IActorSystem {

    <T> IActorRef<T> add(String id, TypeTag<T> type, Function1<IActor<T>, T> supplier);

    /**
     * Get async interface to actor.
     */
    <T> T async(IActorRef<T> receiver);

    /**
     * Stop actor system, actors may complete current work.
     */
    IFuture<Unit> stop();

    /**
     * Stop actor system, interrupt actors current work.
     */
    IFuture<Unit> cancel();

    boolean running();

}