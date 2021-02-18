package mb.statix.concurrent.solver;

import java.util.Map;

import org.immutables.value.Value;

import mb.nabl2.terms.ITerm;
import mb.statix.concurrent.p_raffrayi.IUnitResult;
import mb.statix.scopegraph.terms.Scope;

public interface IStatixGroupResult extends IStatixResult {

    @Value.Parameter public abstract Map<String, IUnitResult<Scope, ITerm, ITerm, GroupResult>> groupResults();

    @Value.Parameter public abstract Map<String, IUnitResult<Scope, ITerm, ITerm, UnitResult>> unitResults();

}
