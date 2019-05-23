package mb.statix.taico.solver;

import java.util.Set;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.immutables.value.Value;

import mb.statix.solver.IConstraint;
import mb.statix.taico.module.IModule;

@Value.Immutable
public abstract class ACompletenessResult {

    @Value.Parameter public abstract boolean isComplete();

    @Value.Parameter @Nullable public abstract IModule cause();
    
    @Value.Default @Nullable public Set<IConstraint> details() {
        return null;
    }
}
