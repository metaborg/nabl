package mb.statix.actors;

public interface IActor<T> extends IActorRef<T> {

    void addMonitor(IActorRef<? extends IActorMonitor> monitor);

    void stop();

}