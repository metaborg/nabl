package mb.statix.solver.tracer;

import mb.nabl2.terms.ITerm;
import mb.statix.constraints.CNew;
import mb.statix.scopegraph.Scope;
import org.immutables.value.Value;

import javax.annotation.Nullable;

@Value.Immutable
public abstract class ACNewStep implements IStep {

    @Value.Parameter @Override public abstract CNew constraint();

    @Value.Parameter @Override public abstract StepResult result();

    @Value.Parameter public abstract @Nullable Scope newScope();

    @Value.Parameter public abstract @Nullable ITerm scopeData();

}
