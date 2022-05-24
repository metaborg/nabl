package mb.p_raffrayi.impl.envdiff;

import org.immutables.value.Value;
import org.metaborg.util.functions.Function1;

import mb.scopegraph.ecoop21.LabelWf;

@Value.Immutable
public abstract class ARemovedEdge<S, L, D> implements IEnvChange<S, L, D> {

    @Value.Parameter public abstract S target();

    @Value.Parameter public abstract LabelWf<L> labelWf();


    @Override public <T> T match(Function1<AddedEdge<S, L, D>, T> onAddedEdge,
            Function1<RemovedEdge<S, L, D>, T> onRemovedEdge) {
        return onRemovedEdge.apply((RemovedEdge<S, L, D>) this);
    }
}
