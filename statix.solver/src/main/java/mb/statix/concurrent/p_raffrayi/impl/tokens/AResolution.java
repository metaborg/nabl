package mb.statix.concurrent.p_raffrayi.impl.tokens;

import org.immutables.value.Value;

import mb.statix.concurrent.actors.futures.IFuture;
import mb.statix.scopegraph.reference.Env;

@Value.Immutable
public abstract class AResolution<S, L, D> implements IWaitFor<S, L, D> {

    @Value.Parameter public abstract IFuture<Env<S, L, D>> future();

}
