package mb.statix.solver.persistent.step;

import io.usethesource.capsule.Map;
import mb.nabl2.terms.ITermVar;
import mb.statix.constraints.CExists;
import org.immutables.value.Value;

@Value.Immutable
public abstract class ACExistsStep implements IStep {

    @Value.Parameter @Override public abstract CExists constraint();

    @Value.Parameter @Override public abstract StepResult result();

    @Value.Parameter public abstract Map.Immutable<ITermVar, ITermVar> existentials();

    @Override public <R> R match(Cases<R> cases) {
        return cases.caseExists((CExistsStep) this);
    }

}
