package mb.statix.concurrent.p_raffrayi;

import java.util.Collection;
import java.util.Map;

public interface IScopeImpl<S, D> {

    S make(String id, String name);

    String id(S scope);

    String name(S scope);

    Collection<S> getAllScopes(D datum);

    D subtituteScopes(D datum, Map<S, S> substitution);

    D embed(S scope);

}