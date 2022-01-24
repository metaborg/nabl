package mb.p_raffrayi.impl.envdiff;

import org.metaborg.util.future.IFuture;
import org.metaborg.util.tuple.Tuple3;

import io.usethesource.capsule.Map;
import io.usethesource.capsule.Set;
import mb.scopegraph.ecoop21.LabelWf;

public class IndexedEnvDiffer<S, L, D> implements IEnvDiffer<S, L, D> {

    private final IEnvDiffer<S, L, D> envDiffer;

    private Map.Transient<Tuple3<S, Set.Immutable<S>, LabelWf<L>>, IFuture<IEnvDiff<S, L, D>>> futures = Map.Transient.of();

    public IndexedEnvDiffer(IEnvDiffer<S, L, D> envDiffer) {
        this.envDiffer = envDiffer;
    }

    @Override public IFuture<IEnvDiff<S, L, D>> diff(S scope, Set.Immutable<S> seenScopes, LabelWf<L> labelWf) {
        final Tuple3<S, Set.Immutable<S>, LabelWf<L>> key = Tuple3.of(scope, seenScopes, labelWf);
        IFuture<IEnvDiff<S, L, D>> result;
        if((result = futures.get(key)) == null) {
            result = envDiffer.diff(scope, labelWf);
            futures.put(key, result);
        }
        return result;
    }

}
