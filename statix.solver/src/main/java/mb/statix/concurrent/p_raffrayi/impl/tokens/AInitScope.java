package mb.statix.concurrent.p_raffrayi.impl.tokens;

import org.immutables.value.Value;

@Value.Immutable(prehash = true)
public abstract class AInitScope<S, L, D> implements IWaitFor<S, L, D> {

    @Value.Parameter public abstract S scope();

    @Override public void visit(Cases<S, L, D> cases) {
        cases.on((InitScope<S, L, D>) this);
    }

}
