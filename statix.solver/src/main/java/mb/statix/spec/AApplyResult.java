package mb.statix.spec;

import java.util.Optional;

import javax.annotation.Nullable;

import org.immutables.serial.Serial;
import org.immutables.value.Value;

import mb.nabl2.terms.unification.ud.Diseq;
import mb.statix.constraints.CExists;
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
    @Value.Parameter public abstract CExists body();

    /**
     * Critical edges that are introduced by the application of this rule.
     */
    @Value.Parameter public abstract @Nullable ICompleteness.Immutable criticalEdges();

}