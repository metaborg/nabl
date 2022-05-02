package mb.p_raffrayi.impl.tokens;


import org.immutables.value.Value;
import org.metaborg.util.future.IFuture;

import mb.p_raffrayi.actors.IActorRef;
import mb.p_raffrayi.impl.IUnit;
import mb.p_raffrayi.impl.confirm.ConfirmResult;
import mb.p_raffrayi.nameresolution.DataWf;
import mb.scopegraph.ecoop21.LabelWf;

@Value.Immutable(prehash = true)
public abstract class AConfirm<S, L, D> implements IWaitFor<S, L, D> {

    @Override @Value.Parameter public abstract IActorRef<? extends IUnit<S, L, D, ?>> origin();

    @Value.Parameter public abstract S scope();

    @Value.Parameter public abstract LabelWf<L> labelWF();

    @Value.Parameter public abstract DataWf<S, L, D> dataWF();

    @Value.Parameter public abstract IFuture<ConfirmResult<S, L, D>> future();

    @Override public void visit(Cases<S, L, D> cases) {
        cases.on((Confirm<S, L, D>) this);
    }

    /*
     * CAREFUL
     * For this class, hashCode and equals are simplified to reference equality for performance.
     * This is possible because we never create a new instance which is used in isWaitingFor.
     * The tokens CloseScope & CloseLabel are created for such checks, and must have structural equality.
     */

    private volatile int hashCode;

    @Override public int hashCode() {
        int result = hashCode;
        if(result == 0) {
            result = super.hashCode();
            hashCode = result;
        }
        return hashCode;
    }

    @Override public boolean equals(Object obj) {
        return this == obj;
    }

}
