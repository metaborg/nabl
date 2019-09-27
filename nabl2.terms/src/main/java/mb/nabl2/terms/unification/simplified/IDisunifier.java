package mb.nabl2.terms.unification.simplified;

import java.util.Map.Entry;
import java.util.Optional;

import mb.nabl2.terms.ITerm;

public interface IDisunifier {

    Optional<? extends IDisunifier>
            disunifyAny(Iterable<? extends Entry<? extends ITerm, ? extends ITerm>> disequalities);

}
