package mb.p_raffrayi;

import java.util.Collection;
import java.util.Map;

public interface IScopeImpl<S, D> {

    S make(String id, String name);

    String id(S scope);

    Collection<S> getAllScopes(D datum);

    D embed(S scope);

    D substituteScopes(D datum, Map<S, S> substitution);

}