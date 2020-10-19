package mb.statix.concurrent.p_raffrayi.impl.tokens;

import org.immutables.value.Value;

import mb.statix.concurrent.actors.futures.IFuture;
import mb.statix.concurrent.p_raffrayi.nameresolution.DataLeq;
import mb.statix.concurrent.p_raffrayi.nameresolution.DataWF;
import mb.statix.concurrent.p_raffrayi.nameresolution.LabelOrder;
import mb.statix.concurrent.p_raffrayi.nameresolution.LabelWF;
import mb.statix.scopegraph.path.IScopePath;
import mb.statix.scopegraph.reference.Env;

@Value.Immutable(prehash = true)
public abstract class AQuery<S, L, D> implements IWaitFor<S, L, D> {

    // @Value.Parameter public abstract IScopePath<S, L> path();

    // @Value.Parameter public abstract LabelWF<L> labelWF();

    // @Value.Parameter public abstract DataWF<D> dataWF();

    // @Value.Parameter public abstract LabelOrder<L> labelOrder();

    // @Value.Parameter public abstract DataLeq<D> dataEquiv();

    @Value.Parameter public abstract IFuture<Env<S, L, D>> future();

    @Override public void visit(Cases<S, L, D> cases) {
        cases.on((Query<S, L, D>) this);
    }

    public static <S, L, D> Query<S, L, D> of(IScopePath<S, L> path, LabelWF<L> labelWF, DataWF<D> dataWF,
            LabelOrder<L> labelOrder, DataLeq<D> dataEquiv, IFuture<Env<S, L, D>> future) {
        return Query.of(future);
    }

}
