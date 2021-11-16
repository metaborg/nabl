package mb.p_raffrayi.impl.envdiff;

import org.immutables.value.Value;
import org.metaborg.util.collection.CapsuleUtil;

import io.usethesource.capsule.Set;
import mb.scopegraph.oopsla20.diff.BiMap;

@Value.Immutable
public abstract class AEnvDiff<S, L, D> implements IEnvDiff<S, L, D> {

    @SuppressWarnings("rawtypes") private static final EnvDiff EMPTY =
            EnvDiff.of(BiMap.Immutable.of(), CapsuleUtil.immutableSet());

    @Value.Parameter public abstract BiMap.Immutable<S> patches();

    @Value.Parameter public abstract Set.Immutable<IEnvChange<S, L, D>> changes();

    @SuppressWarnings("unchecked") public static <S, L, D> EnvDiff<S, L, D> empty() {
        return (EnvDiff<S, L, D>) EMPTY;
    }

}
