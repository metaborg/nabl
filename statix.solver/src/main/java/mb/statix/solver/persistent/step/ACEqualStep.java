package mb.statix.solver.persistent.step;

import mb.nabl2.terms.unification.u.IUnifier;
import mb.statix.constraints.CEqual;
import org.immutables.value.Value;

import jakarta.annotation.Nullable;

@Value.Immutable
public abstract class ACEqualStep implements IStep {

    @Value.Parameter @Override public abstract CEqual constraint();

    @Value.Parameter @Override public abstract StepResult result();

    @Value.Parameter public abstract @Nullable IUnifier.Result<IUnifier.Immutable> unifierResult();

    @Override public <R> R match(Cases<R> cases) {
        return cases.caseEqual((CEqualStep) this);
    }
}
