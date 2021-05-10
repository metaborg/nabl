package mb.p_raffrayi.impl.tokens;

import org.immutables.value.Value;

import mb.p_raffrayi.actors.IActorRef;
import mb.p_raffrayi.impl.IUnit;

@Value.Immutable(prehash = true)
public abstract class ACloseScope<S, L, D> implements IWaitFor<S, L, D> {

    @Override @Value.Parameter public abstract IActorRef<? extends IUnit<S, L, D, ?>> origin();

    @Value.Parameter public abstract S scope();

    @Override public void visit(Cases<S, L, D> cases) {
        cases.on((CloseScope<S, L, D>) this);
    }

}
