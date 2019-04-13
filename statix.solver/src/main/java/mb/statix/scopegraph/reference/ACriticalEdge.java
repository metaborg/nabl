package mb.statix.scopegraph.reference;

import javax.annotation.Nullable;

import org.immutables.value.Value;

import mb.nabl2.terms.ITerm;
import mb.statix.taico.module.IModule;

@Value.Immutable
public abstract class ACriticalEdge {

    @Value.Parameter public abstract ITerm scope();

    @Value.Parameter public abstract ITerm label();
    
    @Value.Parameter @Nullable public abstract IModule cause();

    @Override public String toString() {
        return scope() + "-" + label();
    }

}