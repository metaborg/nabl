package mb.statix.concurrent.p_raffrayi.impl.tokens;

import org.immutables.value.Value;

import mb.statix.concurrent.actors.futures.IFuture;
import mb.statix.scopegraph.path.IScopePath;
import mb.statix.scopegraph.reference.DataLeq;
import mb.statix.scopegraph.reference.DataWF;
import mb.statix.scopegraph.reference.Env;
import mb.statix.scopegraph.reference.LabelOrder;
import mb.statix.scopegraph.reference.LabelWF;

@Value.Immutable(prehash = true)
public abstract class AQuery<S, L, D> implements IWaitFor<S, L, D> {

    @Value.Parameter public abstract IScopePath<S, L> path();

    @Value.Parameter public abstract LabelWF<L> labelWF();

    @Value.Parameter public abstract DataWF<D> dataWF();

    @Value.Parameter public abstract LabelOrder<L> labelOrder();

    @Value.Parameter public abstract DataLeq<D> dataEquiv();

    @Value.Parameter public abstract IFuture<Env<S, L, D>> future();

}
