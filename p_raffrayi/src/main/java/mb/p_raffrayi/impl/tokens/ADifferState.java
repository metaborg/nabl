package mb.p_raffrayi.impl.tokens;

import java.util.Collections;
import java.util.Set;

import org.immutables.value.Value;
import org.metaborg.util.future.ICompletableFuture;

import mb.p_raffrayi.actors.IActorRef;
import mb.p_raffrayi.impl.IUnit;

@Value.Immutable
public abstract class ADifferState<S, L, D> implements IWaitFor<S, L, D> {

    @Override @Value.Parameter public abstract IActorRef<? extends IUnit<S, L, D, ?, ?>> origin();

    @Value.Parameter public abstract Set<S> matches();

    @Value.Parameter public abstract Set<S> diffs();

    @Value.Parameter public abstract ICompletableFuture<?> future();

    @Override public void visit(Cases<S, L, D> cases) {
        cases.on((DifferState<S, L, D>) this);
    }

    public static <S, L, D> DifferState<S, L, D> ofMatch(IActorRef<? extends IUnit<S, L, D, ?, ?>> origin, S scope,
            ICompletableFuture<?> future) {
        return DifferState.of(origin, Collections.singleton(scope), Collections.emptySet(), future);
    }

    public static <S, L, D> DifferState<S, L, D> ofDiff(IActorRef<? extends IUnit<S, L, D, ?, ?>> origin, S scope,
            ICompletableFuture<?> future) {
        return DifferState.of(origin, Collections.emptySet(), Collections.singleton(scope), future);
    }

}
