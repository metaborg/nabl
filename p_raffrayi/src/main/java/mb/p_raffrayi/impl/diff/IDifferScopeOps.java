package mb.p_raffrayi.impl.diff;

import java.util.Optional;

import io.usethesource.capsule.Set.Immutable;
import mb.scopegraph.oopsla20.diff.BiMap;

public interface IDifferScopeOps<S, D> {

    Immutable<S> getScopes(D datum);

    Optional<BiMap.Immutable<S>> matchDatums(D currentDatum, D previousDatum);

}
