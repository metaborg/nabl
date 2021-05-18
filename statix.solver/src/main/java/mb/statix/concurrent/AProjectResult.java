package mb.statix.concurrent;

import java.util.Map;

import javax.annotation.Nullable;

import org.immutables.value.Value;
import org.metaborg.util.unit.Unit;

import com.google.common.base.MoreObjects;

import mb.nabl2.terms.ITerm;
import mb.p_raffrayi.IUnitResult;
import mb.statix.scopegraph.Scope;
import mb.statix.solver.persistent.SolverResult;

@Value.Immutable
public abstract class AProjectResult implements IStatixGroupResult {

    @Value.Parameter public abstract String resource();

    @Value.Parameter public abstract Map<String, IUnitResult<Scope, ITerm, ITerm, Unit>> libraryResults();

    @Override @Value.Parameter public abstract Map<String, IUnitResult<Scope, ITerm, ITerm, GroupResult>> groupResults();

    @Override @Value.Parameter public abstract Map<String, IUnitResult<Scope, ITerm, ITerm, UnitResult>> unitResults();

    @Override @Value.Parameter public abstract @Nullable SolverResult solveResult();

    @Override @Value.Parameter public abstract @Nullable Throwable exception();

    @Override
    public String toString() {
      return MoreObjects.toStringHelper("ProjectResult")
          .omitNullValues()
          .add("resource", resource())
          .add("groupResults", groupResults())
          .add("unitResults", unitResults())
          .add("solveResult", solveResult())
          .add("exception", exception())
          .toString();
    }

}