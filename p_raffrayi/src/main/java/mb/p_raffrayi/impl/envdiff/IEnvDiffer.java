package mb.p_raffrayi.impl.envdiff;

import org.metaborg.util.collection.CapsuleUtil;
import org.metaborg.util.future.IFuture;

import io.usethesource.capsule.Set;
import mb.scopegraph.ecoop21.LabelWf;

public interface IEnvDiffer<S, L, D> {

    default IFuture<IEnvDiff<S, L, D>> diff(S scope, LabelWf<L> labelWf) {
        return diff(scope, CapsuleUtil.immutableSet(scope), labelWf);
    }

    IFuture<IEnvDiff<S, L, D>> diff(S scope, Set.Immutable<S> seenScopes, LabelWf<L> labelWf);

}
