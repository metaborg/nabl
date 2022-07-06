package mb.p_raffrayi.impl.envdiff;

import org.metaborg.util.collection.CapsuleUtil;

import mb.scopegraph.oopsla20.diff.BiMap;

public final class EnvDiffs {

    // Not in `AEnvDiff` to prevent deadlock in class loading.
    @SuppressWarnings("rawtypes") private static final EnvDiff EMPTY =
            EnvDiff.of(BiMap.Immutable.of(), CapsuleUtil.immutableSet());


    private EnvDiffs() {
    }

    @SuppressWarnings("unchecked") public static <S, L, D> EnvDiff<S, L, D> empty() {
        return (EnvDiff<S, L, D>) EMPTY;
    }

}
