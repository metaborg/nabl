package mb.statix.solver.persistent.step;

import mb.statix.constraints.CTry;
import org.immutables.value.Value;

@Value.Immutable
public abstract class ACTryStep implements IStep {

    @Value.Parameter @Override public abstract CTry constraint();

    @Value.Parameter @Override public abstract StepResult result();

    @Override public <R> R match(Cases<R> cases) {
        return cases.caseTry((CTryStep) this);
    }

}
