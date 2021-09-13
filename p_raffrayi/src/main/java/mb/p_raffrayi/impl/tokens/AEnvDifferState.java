package mb.p_raffrayi.impl.tokens;

import org.immutables.value.Value;
import org.metaborg.util.future.ICompletableFuture;

import io.usethesource.capsule.Set;
import mb.p_raffrayi.actors.IActorRef;
import mb.p_raffrayi.impl.IUnit;
import mb.p_raffrayi.nameresolution.DataWf;
import mb.scopegraph.ecoop21.LabelWf;

@Value.Immutable
public abstract class AEnvDifferState<S, L, D> implements IWaitFor<S, L, D> {

    @Override @Value.Parameter public abstract IActorRef<? extends IUnit<S, L, D, ?, ?>> origin();

    @Value.Parameter public abstract S scope();

    @Value.Parameter public abstract Set.Immutable<S> seenScopes();

    @Value.Parameter public abstract LabelWf<L> labelWf();

    @Value.Parameter public abstract DataWf<S, L, D> dataWf();

    @Value.Parameter public abstract ICompletableFuture<?> future();

    @Override public void visit(Cases<S, L, D> cases) {
        cases.on((EnvDifferState<S, L, D>) this);
    }

    @Override public boolean equals(Object obj) {
        return this == obj;
    }

    @Override public int hashCode() {
        return super.hashCode();
    }

}
