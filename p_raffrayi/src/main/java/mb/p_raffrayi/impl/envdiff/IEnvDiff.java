package mb.p_raffrayi.impl.envdiff;

import io.usethesource.capsule.Set;
import mb.scopegraph.oopsla20.diff.BiMap;

public interface IEnvDiff<S, L, D> {

    Set.Immutable<IEnvChange<S, L, D>> changes();

    BiMap.Immutable<S> patches();

}
