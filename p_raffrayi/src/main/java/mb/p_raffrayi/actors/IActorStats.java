package mb.p_raffrayi.actors;

import java.util.Collection;

public interface IActorStats {

    Collection<String> csvHeaders();

    Collection<String> csvRow();

}