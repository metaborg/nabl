package mb.statix.spec;

import java.util.Set;

import org.immutables.serial.Serial;
import org.immutables.value.Value;

import mb.nabl2.terms.ITermVar;
import mb.statix.solver.IConstraint;
import mb.statix.solver.IState;

@Value.Immutable
@Serial.Version(42L)
public abstract class AApplyResult {

    @Value.Parameter public abstract IState.Immutable state();

    @Value.Parameter public abstract Set<ITermVar> updatedVars();

    @Value.Parameter public abstract Set<ITermVar> constrainedVars();

    @Value.Parameter public abstract IConstraint constraint();

}