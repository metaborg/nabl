package mb.statix.solver.persistent.step;

import jakarta.annotation.Nullable;
import mb.nabl2.terms.ITerm;
import mb.statix.constraints.CTellEdge;
import mb.statix.scopegraph.Scope;
import org.immutables.value.Value;

@Value.Immutable
public abstract class ACTellEdgeStep implements IStep {

    @Value.Parameter @Override public abstract CTellEdge constraint();

    @Value.Parameter @Override public abstract StepResult result();

    @Value.Parameter public abstract @Nullable Scope source();

    @Value.Parameter public abstract @Nullable Scope target();

    public ITerm label() {
        return constraint().label();
    }

    @Override public <R> R match(Cases<R> cases) {
        return cases.caseTellEdge((CTellEdgeStep) this);
    }

}
