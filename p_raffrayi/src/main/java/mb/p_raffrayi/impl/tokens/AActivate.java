package mb.p_raffrayi.impl.tokens;

import org.immutables.value.Value;
import org.metaborg.util.future.ICompletableFuture;

import mb.p_raffrayi.actors.IActorRef;
import mb.p_raffrayi.impl.IUnit;

@Value.Immutable
public abstract class AActivate<S, L, D> implements IWaitFor<S, L, D> {

    @Override @Value.Parameter public abstract IActorRef<? extends IUnit<S, L, D, ?>> origin();

    @Value.Parameter public abstract ICompletableFuture<?> future();

    @Override public void visit(Cases<S, L, D> cases) {
        cases.on((Activate<S, L, D>) this);
    }

}
