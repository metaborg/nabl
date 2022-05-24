package mb.p_raffrayi;

import java.util.Map;
import java.util.Optional;

import io.usethesource.capsule.Set.Immutable;
import mb.scopegraph.oopsla20.diff.BiMap;

public interface IScopeImpl<S, D> {

    S make(String id, String name);

    String id(S scope);

    D substituteScopes(D datum, Map<S, S> substitution);

    Immutable<S> getScopes(D datum);

    D embed(S scope);

    Optional<BiMap.Immutable<S>> matchDatums(D currentDatum, D previousDatum);

}
