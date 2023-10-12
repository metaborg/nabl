package mb.statix.concurrent;

import javax.annotation.Nullable;

import org.immutables.serial.Serial;
import org.immutables.value.Value;

import io.usethesource.capsule.Map;
import mb.nabl2.terms.ITerm;
import mb.p_raffrayi.IUnitResult;
import mb.p_raffrayi.impl.Result;
import mb.statix.scopegraph.Scope;
import mb.statix.solver.persistent.SolverResult;

@Value.Immutable
@Serial.Version(42L)
public abstract class AGroupResult implements IStatixResult {

    @Value.Parameter public abstract String resource();

    @Value.Parameter public abstract Map.Immutable<String, IUnitResult<Scope, ITerm, ITerm, Result<Scope, ITerm, ITerm, GroupResult, SolverState>>> groupResults();

    @Value.Parameter public abstract Map.Immutable<String, IUnitResult<Scope, ITerm, ITerm, Result<Scope, ITerm, ITerm, UnitResult, SolverState>>> unitResults();

    @Value.Parameter public abstract @Nullable SolverResult solveResult();

    @Value.Parameter public abstract @Nullable Throwable exception();

}
