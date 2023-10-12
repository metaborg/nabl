package mb.statix.solver.tracer;

import io.usethesource.capsule.Set;
import mb.nabl2.terms.ITermVar;
import mb.statix.constraints.CArith;
import mb.statix.solver.IConstraint;
import mb.statix.solver.IState;
import org.immutables.value.Value;

import java.util.Collection;

@Value.Immutable
public abstract class ACArithStep implements IStep {

    @Value.Parameter @Override public abstract CArith constraint();

    @Value.Parameter @Override public abstract StepResult result();

    @Override
    public <R> R match(Cases<R> cases) {
        return cases.caseArith((CArithStep) this);
    }
}
