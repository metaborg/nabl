package mb.statix.solver.persistent.step;

import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.stratego.TermIndex;
import mb.statix.constraints.CAstProperty;
import mb.statix.solver.ITermProperty;
import org.immutables.value.Value;

import jakarta.annotation.Nullable;

@Value.Immutable
public abstract class ACAstPropertyStep implements IStep {

    @Value.Parameter @Override public abstract CAstProperty constraint();

    @Value.Parameter @Override public abstract StepResult result();

    @Value.Parameter public abstract @Nullable TermIndex index();

    @Value.Parameter public abstract @Nullable ITermProperty property();

    @Value.Parameter public abstract @Nullable ITerm value();

    /**
     * @return true if the property was updated, false if it was not updated.
     *      This can happen in case a singleton property is assigned the same value (with same attachments) multiple times.
     */
    @Value.Default public boolean update() {
        return true;
    }

    public @Nullable ITerm prop() {
        return constraint().property();
    }

    @Override public <R> R match(Cases<R> cases) {
        return cases.caseAstProperty((CAstPropertyStep) this);
    }

}
