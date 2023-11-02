package mb.statix.concurrent;

import jakarta.annotation.Nullable;

import org.immutables.serial.Serial;
import org.immutables.value.Value;

import mb.statix.solver.persistent.SolverResult;

@Value.Immutable
@Serial.Version(42L)
public abstract class AUnitResult implements IStatixResult {

    @Value.Parameter public abstract String resource();

    @Override @Value.Parameter public abstract @Nullable SolverResult solveResult();

    @Override @Value.Parameter public abstract @Nullable Throwable exception();

}