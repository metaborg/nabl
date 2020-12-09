package mb.statix.concurrent.p_raffrayi.impl.tokens;

import org.immutables.value.Value;

import mb.statix.concurrent.actors.IActorRef;
import mb.statix.concurrent.p_raffrayi.impl.IUnit;
import mb.statix.scopegraph.reference.EdgeOrData;

@Value.Immutable(prehash = true)
public abstract class ACloseLabel<S, L, D> implements IWaitFor<S, L, D> {

    @Override @Value.Parameter public abstract IActorRef<? extends IUnit<S, L, D, ?>> origin();

    @Value.Parameter public abstract S scope();

    @Value.Parameter public abstract EdgeOrData<L> label();

    @Override public void visit(Cases<S, L, D> cases) {
        cases.on((CloseLabel<S, L, D>) this);
    }

}