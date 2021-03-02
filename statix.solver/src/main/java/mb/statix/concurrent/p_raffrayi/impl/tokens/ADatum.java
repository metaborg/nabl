package mb.statix.concurrent.p_raffrayi.impl.tokens;

import org.immutables.value.Value;

import mb.statix.concurrent.actors.IActorRef;
import mb.statix.concurrent.p_raffrayi.impl.IUnit;

@Value.Immutable
public abstract class ADatum<S, L, D> implements IWaitFor<S, L, D> {

    @Value.Parameter @Override public abstract IActorRef<? extends IUnit<S, L, D, ?>> origin();

    @Value.Parameter public abstract S scope();

    @Override public void visit(Cases<S, L, D> cases) {
        // TODO Auto-generated method stub
    }

}
