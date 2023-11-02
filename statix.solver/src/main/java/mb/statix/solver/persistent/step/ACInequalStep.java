package mb.statix.solver.persistent.step;

import mb.nabl2.terms.unification.u.IUnifier;
import mb.nabl2.terms.unification.ud.Diseq;
import mb.statix.constraints.CInequal;
import org.immutables.value.Value;

import jakarta.annotation.Nullable;
import java.util.Optional;

@Value.Immutable
public abstract class ACInequalStep implements IStep {

    @Value.Parameter @Override public abstract CInequal constraint();

    @Value.Parameter @Override public abstract StepResult result();

    @Value.Parameter public abstract @Nullable IUnifier.Result<Optional<Diseq>> unifierResult();

    @Override public <R> R match(Cases<R> cases) {
        return cases.caseInequal((CInequalStep) this);
    }

}
