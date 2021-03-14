package mb.statix.concurrent.actors;

public interface IActorStats {

    Iterable<String> csvHeaders();

    Iterable<String> csvRow();

}