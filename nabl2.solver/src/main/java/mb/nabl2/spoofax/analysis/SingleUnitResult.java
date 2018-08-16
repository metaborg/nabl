package mb.nabl2.spoofax.analysis;

import java.util.List;
import java.util.Optional;

import org.immutables.serial.Serial;
import org.immutables.value.Value;

import mb.nabl2.constraints.IConstraint;
import mb.nabl2.solver.ISolution;
import mb.nabl2.terms.ITerm;

@Value.Immutable
@Serial.Version(value = 42L)
public abstract class SingleUnitResult implements IResult {

    @Override public boolean partial() {
        return false;
    }

    @Value.Parameter public abstract List<IConstraint> constraints();

    @Override @Value.Parameter public abstract ISolution solution();

    @Override @Value.Parameter public abstract Optional<ITerm> customAnalysis();

}