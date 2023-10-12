package mb.statix.solver.tracer;

import io.usethesource.capsule.Map;
import io.usethesource.capsule.Set;
import mb.nabl2.terms.ITermVar;
import mb.statix.constraints.CExists;
import mb.statix.solver.IConstraint;
import mb.statix.solver.IState;
import org.immutables.value.Value;

import java.util.Collection;

@Value.Immutable
public abstract class ACExistsStep implements IStep {

    @Value.Parameter @Override public abstract CExists constraint();

    @Value.Parameter @Override public abstract StepResult result();

    @Value.Parameter public abstract Map.Immutable<ITermVar, ITermVar> existentials();

    @Override public <R> R match(Cases<R> cases) {
        return cases.caseExists((CExistsStep) this);
    }

}
