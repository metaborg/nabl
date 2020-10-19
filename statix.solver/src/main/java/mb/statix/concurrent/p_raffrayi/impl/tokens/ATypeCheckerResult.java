package mb.statix.concurrent.p_raffrayi.impl.tokens;

import org.immutables.value.Value;

import mb.statix.concurrent.actors.futures.ICompletableFuture;

@Value.Immutable(prehash = true)
public abstract class ATypeCheckerResult<S, L, D> implements IWaitFor<S, L, D> {

    @Value.Parameter public abstract ICompletableFuture<?> future();

    @Override public void visit(Cases<S, L, D> cases) {
        cases.on((TypeCheckerResult<S, L, D>) this);
    }

}