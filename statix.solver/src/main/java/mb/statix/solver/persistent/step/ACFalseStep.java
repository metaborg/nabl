package mb.statix.solver.persistent.step;

import mb.statix.constraints.CFalse;
import org.immutables.value.Value;

@Value.Immutable
public abstract class ACFalseStep implements IStep {

    @Value.Parameter @Override public abstract CFalse constraint();

    @Value.Parameter @Override public abstract StepResult result();

    @Override public <R> R match(Cases<R> cases) {
        return cases.caseFalse((CFalseStep) this);
    }

}
