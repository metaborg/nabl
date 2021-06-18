package mb.p_raffrayi;

import java.util.Map;

public interface IScopeImpl<S, D> {

    S make(String id, String name);

    String id(S scope);

    D substituteScopes(D datum, Map<S, S> substitution);

}