package mb.statix.concurrent.p_raffrayi.impl.tokens;

import org.immutables.value.Value;

@Value.Immutable
public abstract class ACloseScope<S, L, D> implements IWaitFor<S, L, D> {

    @Value.Parameter public abstract S scope();

}