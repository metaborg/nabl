package mb.statix.solver.persistent.step;

import mb.statix.constraints.CTrue;
import org.immutables.value.Value;

@Value.Immutable
public abstract class ACTrueStep implements IStep {

    @Value.Parameter @Override public abstract CTrue constraint();

    @Value.Parameter @Override public abstract StepResult result();

    @Override public <R> R match(Cases<R> cases) {
        return cases.caseTrue((CTrueStep) this);
    }

}
