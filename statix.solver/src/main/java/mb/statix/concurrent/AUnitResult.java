package mb.statix.concurrent;

import javax.annotation.Nullable;

import org.immutables.value.Value;

import mb.statix.solver.persistent.SolverResult;

@Value.Immutable
public abstract class AUnitResult implements IStatixResult {

    @Value.Parameter public abstract String resource();

    @Override @Value.Parameter public abstract @Nullable SolverResult solveResult();

    @Override @Value.Parameter public abstract @Nullable Throwable exception();

}