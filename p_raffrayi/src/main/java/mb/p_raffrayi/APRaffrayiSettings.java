package mb.p_raffrayi;

import org.immutables.value.Value;

@Value.Immutable
public abstract class APRaffrayiSettings {

    @Value.Parameter public abstract boolean incrementalDeadlock();

    @Value.Parameter public abstract boolean scopeGraphDiff();

    public boolean incremental() {
        return incrementalDeadlock() || scopeGraphDiff();
    }

}
