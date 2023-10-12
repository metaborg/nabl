package mb.statix.solver.tracer;

import mb.nabl2.terms.unification.u.IUnifier;
import mb.statix.constraints.CEqual;
import org.immutables.value.Value;

import javax.annotation.Nullable;

@Value.Immutable
public abstract class ACEqualStep implements IStep {

    @Value.Parameter @Override public abstract CEqual constraint();

    @Value.Parameter @Override public abstract StepResult result();

    @Value.Parameter public abstract @Nullable IUnifier.Result<IUnifier.Immutable> unifierResult();

}
