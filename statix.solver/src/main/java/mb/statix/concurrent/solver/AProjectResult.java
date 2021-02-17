package mb.statix.concurrent.solver;

import java.util.Map;

import javax.annotation.Nullable;

import org.immutables.value.Value;

import mb.nabl2.terms.ITerm;
import mb.statix.concurrent.p_raffrayi.IUnitResult;
import mb.statix.scopegraph.terms.Scope;
import mb.statix.solver.persistent.SolverResult;

@Value.Immutable
public abstract class AProjectResult implements IStatixGroupResult {

    @Value.Parameter public abstract String resource();

    @Override @Value.Parameter public abstract Map<String, IUnitResult<Scope, ITerm, ITerm, GroupResult>> groupResults();

    @Override @Value.Parameter public abstract Map<String, IUnitResult<Scope, ITerm, ITerm, UnitResult>> unitResults();

    @Override @Value.Parameter public abstract @Nullable SolverResult solveResult();

    @Override @Value.Parameter public abstract @Nullable Throwable exception();

}