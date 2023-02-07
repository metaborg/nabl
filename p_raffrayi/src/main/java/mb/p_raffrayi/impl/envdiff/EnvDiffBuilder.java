package mb.p_raffrayi.impl.envdiff;

import org.metaborg.util.collection.CapsuleUtil;

import io.usethesource.capsule.Set;
import org.metaborg.util.collection.BiMap;

public class EnvDiffBuilder<S, L, D> {

    private final BiMap.Transient<S> patches = BiMap.Transient.of();
    private final Set.Transient<IEnvChange<S, L, D>> changes = CapsuleUtil.transientSet();

    public EnvDiffBuilder(S oldScope, S newScope) {
        patches.put(newScope, oldScope);
    }

    public EnvDiffBuilder<S, L, D> addChange(IEnvChange<S, L, D> change) {
        changes.__insert(change);
        return this;
    }

    public EnvDiffBuilder<S, L, D> addEnvDiff(IEnvDiff<S, L, D> diff) {
        changes.__insertAll(diff.changes());
        patches.putAll(diff.patches());
        return this;
    }

    public EnvDiffBuilder<S, L, D> addPatch(S oldScope, S newScope) {
        patches.put(newScope, oldScope);
        return this;
    }

    public IEnvDiff<S, L, D> build() {
        return EnvDiff.of(patches.freeze(), changes.freeze());
    }

}
