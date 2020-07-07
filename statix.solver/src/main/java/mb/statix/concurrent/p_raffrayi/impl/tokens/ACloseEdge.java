package mb.statix.concurrent.p_raffrayi.impl.tokens;

import org.immutables.value.Value;

import mb.statix.scopegraph.reference.EdgeOrData;

@Value.Immutable
public abstract class ACloseEdge<S, L, D> implements IWaitFor<S, L, D> {

    @Value.Parameter public abstract S scope();

    @Value.Parameter public abstract EdgeOrData<L> edge();

}
