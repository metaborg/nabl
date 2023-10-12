package mb.p_raffrayi.impl.tokens;

import org.immutables.value.Value;
import org.metaborg.util.future.ICompletableFuture;

import mb.p_raffrayi.actors.IActorRef;
import mb.p_raffrayi.impl.IUnit;
import mb.scopegraph.ecoop21.LabelWf;

@Value.Immutable(prehash = false)
public abstract class AEnvDifferState<S, L, D> implements IWaitFor<S, L, D> {

    @Override @Value.Parameter public abstract IActorRef<? extends IUnit<S, L, D, ?>> origin();

    @Value.Parameter public abstract S scope();

    @Value.Parameter public abstract LabelWf<L> labelWf();

    @Value.Parameter public abstract ICompletableFuture<?> future();

    @Override public void visit(Cases<S, L, D> cases) {
        cases.on((EnvDifferState<S, L, D>) this);
    }

    private volatile int hashCode;

    @Override public int hashCode() {
        int result = hashCode;
        if(result == 0) {
            result = super.hashCode();
            hashCode = result;
        }
        return hashCode;
    }

    @Override public boolean equals(Object obj) {
        return this == obj;
    }

}
