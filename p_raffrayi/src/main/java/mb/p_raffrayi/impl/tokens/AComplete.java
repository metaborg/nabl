package mb.p_raffrayi.impl.tokens;

import org.immutables.value.Value;

import mb.p_raffrayi.actors.IActorRef;
import mb.p_raffrayi.impl.IUnit;
import mb.scopegraph.oopsla20.reference.EdgeOrData;

@Value.Immutable
public abstract class AComplete<S, L, D> implements IWaitFor<S, L, D> {

    @Value.Parameter @Override public abstract IActorRef<? extends IUnit<S, L, D, ?>> origin();

    @Value.Parameter public abstract S scope();

    @Value.Parameter public abstract EdgeOrData<L> label();

    @Override public void visit(Cases<S, L, D> cases) {
        cases.on((Complete<S, L, D>) this);
    }

}
