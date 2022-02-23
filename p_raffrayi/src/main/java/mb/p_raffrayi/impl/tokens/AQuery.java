package mb.p_raffrayi.impl.tokens;

import org.immutables.value.Value;
import org.metaborg.util.future.IFuture;

import mb.p_raffrayi.actors.IActorRef;
import mb.p_raffrayi.impl.IQueryAnswer;
import mb.p_raffrayi.impl.IUnit;
import mb.p_raffrayi.nameresolution.DataLeq;
import mb.p_raffrayi.nameresolution.DataWf;
import mb.p_raffrayi.nameresolution.IQuery;
import mb.scopegraph.oopsla20.path.IScopePath;

@Value.Immutable(prehash = false)
public abstract class AQuery<S, L, D> implements IWaitFor<S, L, D> {

    @Override @Value.Parameter public abstract IActorRef<? extends IUnit<S, L, D, ?>> origin();

    @Value.Parameter public abstract IScopePath<S, L> path();

    // @Value.Parameter public abstract LabelWF<L> labelWF();

    @Value.Parameter public abstract DataWf<S, L, D> dataWF();

    // @Value.Parameter public abstract LabelOrder<L> labelOrder();

    // @Value.Parameter public abstract DataLeq<D> dataEquiv();

    @Value.Parameter public abstract IFuture<IQueryAnswer<S, L, D>> future();

    @Override public void visit(Cases<S, L, D> cases) {
        cases.on((Query<S, L, D>) this);
    }

    public static <S, L, D> Query<S, L, D> of(IActorRef<? extends IUnit<S, L, D, ?>> origin, IScopePath<S, L> path,
            @SuppressWarnings("unused") IQuery<S, L, D> query, DataWf<S, L, D> dataWF,
            @SuppressWarnings("unused") DataLeq<S, L, D> dataEquiv, IFuture<IQueryAnswer<S, L, D>> future) {
        return Query.of(origin, path, dataWF, future);
    }

    /*
     * CAREFUL
     * For this class, hashCode and equals are simplified to reference equality for performance.
     * This is possible because we never create a new instance which is used in isWaitingFor.
     * The tokens CloseScope & CloseLabel are created for such checks, and must have structural equality.
     */

    @Override public int hashCode() {
        return super.hashCode();
    }

    @Override public boolean equals(Object obj) {
        return this == obj;
    }

}
