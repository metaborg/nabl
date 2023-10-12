package mb.statix.solver.tracer;

import mb.statix.constraints.CTrue;
import org.immutables.value.Value;

@Value.Immutable
public abstract class ACTrueStep implements IStep {

    @Value.Parameter @Override public abstract CTrue constraint();

    @Value.Parameter @Override public abstract StepResult result();

}
