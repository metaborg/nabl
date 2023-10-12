package mb.statix.solver.tracer;

import mb.statix.constraints.CTry;
import org.immutables.value.Value;

@Value.Immutable
public abstract class ACTryStep implements IStep {

    @Value.Parameter @Override public abstract CTry constraint();

    @Value.Parameter @Override public abstract StepResult result();

}
