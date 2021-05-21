package mb.statix.concurrent;

import java.util.Map;

import javax.annotation.Nullable;

import org.immutables.serial.Serial;
import org.immutables.value.Value;

import mb.nabl2.terms.ITerm;
import mb.p_raffrayi.IUnitResult;
import mb.statix.scopegraph.Scope;
import mb.statix.solver.persistent.SolverResult;

@Value.Immutable
@Serial.Version(42L)
public abstract class AGroupResult implements IStatixGroupResult {

    @Value.Parameter public abstract Map<String, IUnitResult<Scope, ITerm, ITerm, GroupResult>> groupResults();

    @Value.Parameter public abstract Map<String, IUnitResult<Scope, ITerm, ITerm, UnitResult>> unitResults();

    @Override @Value.Parameter public abstract @Nullable SolverResult solveResult();

    @Override @Value.Parameter public abstract @Nullable Throwable exception();

}