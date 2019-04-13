package mb.statix.taico.solver;

import javax.annotation.Nullable;

import org.immutables.value.Value;

import mb.statix.taico.module.IModule;

@Value.Immutable
public abstract class ACompletenessResult {

    @Value.Parameter public abstract boolean isComplete();

    @Value.Parameter @Nullable public abstract IModule cause();
}
