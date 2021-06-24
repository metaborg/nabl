package mb.p_raffrayi.impl.tokens;

import org.immutables.value.Value;

import mb.p_raffrayi.actors.IActorRef;
import mb.p_raffrayi.impl.IUnit;

@Value.Immutable
public abstract class ADatum<S, L, D> implements IWaitFor<S, L, D> {

    @Value.Parameter @Override public abstract IActorRef<? extends IUnit<S, L, D, ?>> origin();

    @Value.Parameter public abstract S scope();

    @Override public void visit(Cases<S, L, D> cases) {
        cases.on((Datum<S, L, D>) this);
    }

}
