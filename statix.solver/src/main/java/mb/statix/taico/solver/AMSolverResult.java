package mb.statix.taico.solver;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.immutables.serial.Serial;
import org.immutables.value.Value;

import mb.nabl2.terms.ITermVar;
import mb.nabl2.terms.unification.IUnifier.Immutable;
import mb.statix.solver.Delay;
import mb.statix.solver.IConstraint;
import mb.statix.solver.ISolverResult;

@Value.Immutable
@Serial.Version(42L)
public abstract class AMSolverResult implements ISolverResult {

    @Value.Parameter public abstract IMState state();
    
    @Value.Default public SolverContext context() {
        return SolverContext.context();
    }

    @Override @Value.Parameter public abstract List<IConstraint> errors();

    @Override @Value.Parameter public abstract Map<IConstraint, Delay> delays();

    @Override @Value.Parameter public abstract Map<ITermVar, ITermVar> existentials();

    @Override
    public Immutable unifier() {
        return state().unifier();
    }
    
    /**
     * Resets all errors and delays on this solver result.
     * 
     * @return
     *      a new solver result
     */
    public MSolverResult reset() {
        return MSolverResult.of(state(), new HashSet<>(), new HashMap<>(), existentials());
    }
}
