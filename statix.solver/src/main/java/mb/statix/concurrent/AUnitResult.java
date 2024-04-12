package mb.statix.concurrent;

import jakarta.annotation.Nullable;

import org.immutables.serial.Serial;
import org.immutables.value.Value;

import mb.statix.solver.persistent.SolverResult;
import mb.statix.solver.tracer.SolverTracer;

@Value.Immutable
@Serial.Version(42L)
public abstract class AUnitResult<TR extends SolverTracer.IResult<TR>> implements IStatixResult<TR> {

    @Value.Parameter public abstract String resource();

    @Override @Value.Parameter public abstract @Nullable SolverResult<TR> solveResult();

    @Override @Value.Parameter public abstract @Nullable Throwable exception();

}
