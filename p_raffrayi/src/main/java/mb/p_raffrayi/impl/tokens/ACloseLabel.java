package mb.p_raffrayi.impl.tokens;

import org.immutables.value.Value;

import mb.p_raffrayi.actors.IActorRef;
import mb.p_raffrayi.impl.IUnit;
import mb.scopegraph.oopsla20.reference.EdgeOrData;

@Value.Immutable(prehash = true)
public abstract class ACloseLabel<S, L, D> implements IWaitFor<S, L, D> {

    @Override @Value.Parameter public abstract IActorRef<? extends IUnit<S, L, D, ?>> origin();

    @Value.Parameter public abstract S scope();

    @Value.Parameter public abstract EdgeOrData<L> label();

    @Override public void visit(Cases<S, L, D> cases) {
        cases.on((CloseLabel<S, L, D>) this);
    }

}
