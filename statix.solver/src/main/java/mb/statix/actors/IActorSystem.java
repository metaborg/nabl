package mb.statix.actors;

import org.metaborg.util.functions.Function1;


public interface IActorSystem {

    <T> IActor<T> add(String id, TypeTag<T> type, Function1<IActor<T>, T> supplier);

    <T> T async(IActorRef<T> receiver);

    void start();

    void stop();

}