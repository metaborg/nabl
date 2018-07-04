package mb.nabl2.spoofax.analysis;

import java.util.Set;

import org.immutables.serial.Serial;
import org.immutables.value.Value;

import mb.nabl2.constraints.IConstraint;
import mb.nabl2.scopegraph.terms.Scope;
import mb.nabl2.solver.Fresh;
import mb.nabl2.solver.ISolution;
import mb.nabl2.terms.ITermVar;

@Value.Immutable
@Serial.Version(value = 42L)
public abstract class InitialResult {

    @Value.Parameter public abstract IConstraint constraint();

    @Value.Parameter public abstract ISolution solution();

    @Value.Parameter public abstract Set<ITermVar> globalVars();

    @Value.Parameter public abstract Set<Scope> globalScopes();

    @Value.Parameter public abstract Fresh fresh();

}