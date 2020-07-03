package mb.statix.actors;

import org.metaborg.util.functions.Function1;

public interface IActor<T> extends IActorRef<T> {

    <U> IActor<U> add(String id, TypeTag<U> type, Function1<IActor<U>, U> supplier);

    <U> U async(IActorRef<U> other);

    void addMonitor(IActorMonitor monitor);

    void stop();

}