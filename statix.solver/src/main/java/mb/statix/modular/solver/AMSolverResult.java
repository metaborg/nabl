package mb.statix.modular.solver;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.immutables.serial.Serial;
import org.immutables.value.Value;

import mb.nabl2.terms.ITermVar;
import mb.statix.modular.solver.state.IMState;
import mb.statix.solver.Delay;
import mb.statix.solver.IConstraint;
import mb.statix.solver.ISolverResult;
import mb.statix.taico.solver.MSolverResult;

@Value.Immutable
@Serial.Version(42L)
public abstract class AMSolverResult implements ISolverResult {

    @Override @Value.Parameter public abstract IMState state();

    @Override @Value.Parameter public abstract List<IConstraint> errors();

    @Override @Value.Parameter public abstract Map<IConstraint, Delay> delays();

    @Override @Value.Parameter public abstract Map<ITermVar, ITermVar> existentials();

    @Value.Default public Context context() {
        return Context.context();
    }

    /**
     * Resets all errors and delays on this solver result.
     * 
     * @return
     *      a new solver result
     */
    public MSolverResult reset() {
        return MSolverResult.builder().context(context()).state(state()).existentials(existentials()).delays(new HashMap<>()).build();
    }
}
