package mb.p_raffrayi.impl.envdiff;

import org.metaborg.util.future.IFuture;

import io.usethesource.capsule.Set;
import mb.p_raffrayi.nameresolution.DataWf;
import mb.scopegraph.ecoop21.LabelWf;


public interface IEnvDiffer<S, L, D> {

    IFuture<IEnvDiff<S, L, D>> diff(S scope, LabelWf<L> labelWf, DataWf<S, L, D> dataWf);

    IFuture<IEnvDiff<S, L, D>> diff(S scope, Set.Immutable<S> seenScopes, LabelWf<L> labelWf, DataWf<S, L, D> dataWf);

}