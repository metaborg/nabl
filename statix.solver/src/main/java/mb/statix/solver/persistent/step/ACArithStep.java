package mb.statix.solver.persistent.step;

import mb.statix.constraints.CArith;
import org.immutables.value.Value;

@Value.Immutable
public abstract class ACArithStep implements IStep {

    @Value.Parameter @Override public abstract CArith constraint();

    @Value.Parameter @Override public abstract StepResult result();

    @Override
    public <R> R match(Cases<R> cases) {
        return cases.caseArith((CArithStep) this);
    }
}
