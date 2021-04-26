package mb.p_raffrayi.actors;

public interface IActorStats {

    Iterable<String> csvHeaders();

    Iterable<String> csvRow();

}