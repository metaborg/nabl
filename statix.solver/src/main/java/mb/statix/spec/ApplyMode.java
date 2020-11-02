package mb.statix.spec;

import java.util.List;
import java.util.Optional;

import javax.annotation.Nullable;

import mb.nabl2.terms.ITerm;
import mb.nabl2.util.VoidException;
import mb.statix.solver.Delay;
import mb.statix.solver.IConstraint;
import mb.statix.solver.IState;

public abstract class ApplyMode<E extends Throwable> {

    /**
     * Strict application mode, which throws a Delay exception if the match cannot be decided because of external
     * variables. In the result, {@link ApplyResult#diff()}} and {@link ApplyResult#guard()} are always empty, and
     * {@link ApplyResult#state()} is unchanged from the input.
     */
    public static final ApplyMode<Delay> STRICT = new ApplyStrict();

    /**
     * Relaxed application mode, which succeeds even if external variables need to be unified. In the result,
     * {@link ApplyResult#diff()}} and {@link ApplyResult#guard()} may not be empty.
     */
    public static final ApplyMode<VoidException> RELAXED = new ApplyRelaxed();

    abstract Optional<ApplyResult> apply(IState.Immutable state, Rule rule, List<? extends ITerm> args,
            @Nullable IConstraint cause) throws E;

}