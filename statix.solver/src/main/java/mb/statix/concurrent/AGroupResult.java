package mb.statix.concurrent;

import jakarta.annotation.Nullable;

import org.immutables.serial.Serial;
import org.immutables.value.Value;

import io.usethesource.capsule.Map;
import mb.nabl2.terms.ITerm;
import mb.p_raffrayi.IUnitResult;
import mb.p_raffrayi.impl.Result;
import mb.statix.scopegraph.Scope;
import mb.statix.solver.persistent.SolverResult;
import mb.statix.solver.tracer.SolverTracer;

@Value.Immutable
@Serial.Version(42L)
public abstract class AGroupResult<TR extends SolverTracer.IResult<TR>> implements IStatixResult<TR> {

    @Value.Parameter public abstract String resource();

    @Value.Parameter public abstract Map.Immutable<String, IUnitResult<Scope, ITerm, ITerm, Result<Scope, ITerm, ITerm, GroupResult<TR>, SolverState>>> groupResults();

    @Value.Parameter public abstract Map.Immutable<String, IUnitResult<Scope, ITerm, ITerm, Result<Scope, ITerm, ITerm, UnitResult<TR>, SolverState>>> unitResults();

    @Value.Parameter public abstract @Nullable SolverResult<TR> solveResult();

    @Value.Parameter public abstract @Nullable Throwable exception();

}
