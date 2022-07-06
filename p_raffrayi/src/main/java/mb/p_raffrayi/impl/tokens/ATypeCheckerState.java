package mb.p_raffrayi.impl.tokens;

import java.util.List;

import org.immutables.value.Value;
import org.metaborg.util.future.ICompletableFuture;

import mb.p_raffrayi.actors.IActorRef;
import mb.p_raffrayi.impl.IUnit;

@Value.Immutable(prehash = false)
public abstract class ATypeCheckerState<S, L, D> implements IWaitFor<S, L, D> {

    @Override @Value.Parameter public abstract IActorRef<? extends IUnit<S, L, D, ?>> origin();

    @Value.Parameter public abstract List<D> datums();

    @Value.Parameter public abstract ICompletableFuture<?> future();

    @Override public void visit(Cases<S, L, D> cases) {
        cases.on((TypeCheckerState<S, L, D>) this);
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
