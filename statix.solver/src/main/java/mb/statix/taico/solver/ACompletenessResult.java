package mb.statix.taico.solver;

import org.checkerframework.checker.nullness.qual.Nullable;

import org.immutables.value.Value;

import mb.statix.taico.module.IModule;

@Value.Immutable
public abstract class ACompletenessResult {

    @Value.Parameter public abstract boolean isComplete();

    @Value.Parameter @Nullable public abstract IModule cause();
}
