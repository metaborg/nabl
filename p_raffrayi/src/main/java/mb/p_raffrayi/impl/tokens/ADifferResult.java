package mb.p_raffrayi.impl.tokens;

import org.immutables.value.Value;
import org.metaborg.util.future.ICompletableFuture;

import mb.p_raffrayi.actors.IActorRef;
import mb.p_raffrayi.impl.IUnit;

@Value.Immutable(prehash = false)
public abstract class ADifferResult<S, L, D> implements IWaitFor<S, L, D> {

    @Value.Parameter @Override public abstract IActorRef<? extends IUnit<S, L, D, ?>> origin();

    @Value.Parameter public abstract ICompletableFuture<?> future();

    @Override public void visit(Cases<S, L, D> cases) {
        cases.on((DifferResult<S, L, D>) this);
    }

    @Override public int hashCode() {
        return super.hashCode();
    }

    @Override public boolean equals(Object obj) {
        return this == obj;
    }

}
