package mb.p_raffrayi.impl.envdiff;

import org.immutables.value.Value;

import io.usethesource.capsule.Set;
import mb.scopegraph.oopsla20.diff.BiMap;

@Value.Immutable
public abstract class AEnvDiff<S, L, D> implements IEnvDiff<S, L, D> {

    @Value.Parameter public abstract BiMap.Immutable<S> patches();

    @Value.Parameter public abstract Set.Immutable<IEnvChange<S, L, D>> changes();

}
