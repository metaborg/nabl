package mb.statix.spec;

import java.util.List;
import java.util.Optional;

import jakarta.annotation.Nullable;

import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.unification.ud.IUniDisunifier;
import mb.nabl2.util.VoidException;
import mb.statix.solver.Delay;
import mb.statix.solver.IConstraint;

public abstract class ApplyMode<E extends Throwable> {

    public enum Safety {
        SAFE, UNSAFE
    }

    /**
     * Strict application mode, which throws a Delay exception if the match cannot be decided because of external
     * variables. In the result, {@link ApplyResult#guard()} is always empty.
     */
    public static final ApplyMode<Delay> STRICT = new ApplyStrict();

    /**
     * Relaxed application mode, which succeeds even if external variables need to be unified. In the result,
     * {@link ApplyResult#diff()}} and {@link ApplyResult#guard()} may not be empty.
     */
    public static final ApplyMode<VoidException> RELAXED = new ApplyRelaxed();

    abstract Optional<ApplyResult> apply(
            IUniDisunifier.Immutable unifier,
            Rule rule,
            List<? extends ITerm> args,
            @Nullable IConstraint cause,
            Safety safety,
            boolean trackOrigins
    ) throws E;

}