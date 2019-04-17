package mb.nabl2.spoofax.analysis;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.immutables.serial.Serial;
import org.immutables.value.Value;

import mb.nabl2.constraints.IConstraint;
import mb.nabl2.scopegraph.terms.Scope;
import mb.nabl2.solver.Fresh;
import mb.nabl2.solver.ISolution;
import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.ITermVar;

@Value.Immutable
@Serial.Version(value = 42L)
public abstract class MultiInitialResult implements IResult {

    @Override public boolean partial() {
        return true;
    }

    @Override @Value.Parameter public abstract List<IConstraint> constraints();

    @Override @Value.Parameter public abstract ISolution solution();

    @Override @Value.Parameter public abstract Optional<ITerm> customAnalysis();

    @Value.Parameter public abstract Set<ITermVar> globalVars();

    @Value.Parameter public abstract Set<Scope> globalScopes();

    @Override @Value.Parameter public abstract Fresh.Immutable fresh();

}