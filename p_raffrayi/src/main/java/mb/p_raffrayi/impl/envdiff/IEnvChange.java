package mb.p_raffrayi.impl.envdiff;

import org.metaborg.util.functions.Function1;

public interface IEnvChange<S, L, D> {

    <T> T match(Function1<AddedEdge<S, L, D>, T> onAddedEdge, Function1<RemovedEdge<S, L, D>, T> onRemovedEdge);

}
