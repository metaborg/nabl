package mb.statix.concurrent.p_raffrayi.impl.tokens;

import org.immutables.value.Value;

import mb.statix.concurrent.actors.IActorRef;
import mb.statix.concurrent.actors.futures.IFuture;
import mb.statix.concurrent.p_raffrayi.impl.IUnit;
import mb.statix.concurrent.p_raffrayi.nameresolution.DataLeq;
import mb.statix.concurrent.p_raffrayi.nameresolution.DataWf;
import mb.statix.concurrent.p_raffrayi.nameresolution.LabelOrder;
import mb.statix.concurrent.p_raffrayi.nameresolution.LabelWF;
import mb.statix.scopegraph.path.IScopePath;
import mb.statix.scopegraph.reference.Env;

@Value.Immutable(prehash = true)
public abstract class AQuery<S, L, D> implements IWaitFor<S, L, D> {

    @Override @Value.Parameter public abstract IActorRef<? extends IUnit<S, L, D, ?>> origin();

    @Value.Parameter public abstract IScopePath<S, L> path();

    // @Value.Parameter public abstract LabelWF<L> labelWF();

    @Value.Parameter public abstract DataWf<D> dataWF();

    // @Value.Parameter public abstract LabelOrder<L> labelOrder();

    // @Value.Parameter public abstract DataLeq<D> dataEquiv();

    @Value.Parameter public abstract IFuture<Env<S, L, D>> future();

    @Override public void visit(Cases<S, L, D> cases) {
        cases.on((Query<S, L, D>) this);
    }

    public static <S, L, D> Query<S, L, D> of(IActorRef<? extends IUnit<S, L, D, ?>> origin, IScopePath<S, L> path,
            LabelWF<L> labelWF, DataWf<D> dataWF, LabelOrder<L> labelOrder, DataLeq<D> dataEquiv,
            IFuture<Env<S, L, D>> future) {
        return Query.of(origin, path, dataWF, future);
    }

    /*
     * CAREFUL
     * For this class, hashCode and equals are simplified to reference equality for performance.
     * This is possible because we never create a new instance which is used in isWaitingFor.
     * The tokens CloseScope & CloseLabel are created for such checks, and must have structural equality.
     */

    @Override public int hashCode() {
        return System.identityHashCode(this);
    }

    @Override public boolean equals(Object obj) {
        return this == obj;
    }

}