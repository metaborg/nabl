package mb.nabl2.terms.substitution;

import java.util.Map;
import java.util.Set;

import mb.nabl2.terms.ITerm;

public interface IReplacement {

    ITerm replace(ITerm term);

    ITerm apply(ITerm term);

    boolean isEmpty();

    Set<ITerm> keySet();

    Set<ITerm> valueSet();

    Set<? extends Map.Entry<ITerm, ITerm>> entrySet();

}
