package mb.statix.actors;

public interface IActorContext {

    <T> T async(IActorRef<T> receiver);

}