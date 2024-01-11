package mb.statix.spec;

import java.util.Optional;

import mb.nabl2.terms.substitution.ISubstitution;
import org.immutables.serial.Serial;
import org.immutables.value.Value;

import mb.nabl2.terms.unification.ud.Diseq;
import mb.statix.solver.IConstraint;
import mb.statix.solver.completeness.ICompleteness;

@Value.Immutable
@Serial.Version(42L)
public abstract class AApplyResult {

    /**
     * Guard constraint that, if it holds, would prevent this application. If no guard is present, the rule application
     * is unconditional in the given state. The domain of the guard are variables that pre-existed the rule application.
     */
    @Value.Parameter public abstract Optional<Diseq> guard();

    /**
     * The applied rule body.
     */
    @Value.Parameter public abstract IConstraint body();

    /**
     * Critical edges that are introduced by the application of this rule.
     */
    @Value.Parameter public abstract ICompleteness.Immutable criticalEdges();

    /**
     * Substitution that is applied to the rule body in order to generate {@link #body()}.
     */
    @Value.Parameter public abstract ISubstitution.Immutable substitution();

}