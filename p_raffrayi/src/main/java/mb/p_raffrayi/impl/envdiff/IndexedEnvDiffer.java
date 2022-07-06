package mb.p_raffrayi.impl.envdiff;

import java.util.HashMap;
import java.util.Map;

import org.metaborg.util.future.IFuture;
import org.metaborg.util.tuple.Tuple2;

import mb.scopegraph.ecoop21.LabelWf;

public class IndexedEnvDiffer<S, L, D> implements IEnvDiffer<S, L, D> {

    private final IEnvDiffer<S, L, D> envDiffer;

    private Map<Tuple2<S, LabelWf<L>>, IFuture<IEnvDiff<S, L, D>>> futures = new HashMap<>();

    public IndexedEnvDiffer(IEnvDiffer<S, L, D> envDiffer) {
        this.envDiffer = envDiffer;
    }

    @Override public IFuture<IEnvDiff<S, L, D>> diff(S scope, LabelWf<L> labelWf) {
        final Tuple2<S, LabelWf<L>> key = Tuple2.of(scope, labelWf);
        IFuture<IEnvDiff<S, L, D>> result;
        if((result = futures.get(key)) == null) {
            result = envDiffer.diff(scope, labelWf);
            futures.put(key, result);
        }
        return result;
    }

}
