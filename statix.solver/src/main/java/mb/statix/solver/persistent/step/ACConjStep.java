package mb.statix.solver.persistent.step;

import mb.statix.constraints.CConj;
import org.immutables.value.Value;

@Value.Immutable
public abstract class ACConjStep implements IStep {

    @Value.Parameter @Override public abstract CConj constraint();

    @Value.Parameter @Override public abstract StepResult result();

    @Override public <R> R match(Cases<R> cases) {
        return cases.caseConj((CConjStep) this);
    }

}
