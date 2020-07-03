package mb.statix.actors;

import org.metaborg.util.functions.Function1;


public interface IActorSystem {

    <T> IActorRef<T> add(String id, TypeTag<T> type, Function1<IActor<T>, T> supplier);

    <T> T async(IActorRef<T> receiver);

    void addMonitor(IActorRef<?> actor, IActorRef<? extends IActorMonitor> monitor);

    void start();

    void stop();

}