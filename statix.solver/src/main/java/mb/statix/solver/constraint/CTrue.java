package mb.statix.solver.constraint;

import java.util.Optional;

import javax.annotation.Nullable;

import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.substitution.ISubstitution;
import mb.nabl2.util.TermFormatter;
import mb.statix.solver.ConstraintContext;
import mb.statix.solver.ConstraintResult;
import mb.statix.solver.IConstraint;
import mb.statix.solver.State;
import mb.statix.taico.solver.MConstraintContext;
import mb.statix.taico.solver.MConstraintResult;
import mb.statix.taico.solver.MState;

/**
 * Implementation for the true constraint.
 * 
 * <pre>true</pre>
 */
public class CTrue implements IConstraint {

    private final @Nullable IConstraint cause;

    /**
     * Creates a new true constraint without a cause.
     */
    public CTrue() {
        this(null);
    }

    public CTrue(@Nullable IConstraint cause) {
        this.cause = cause;
    }

    @Override public Optional<IConstraint> cause() {
        return Optional.ofNullable(cause);
    }

    @Override public CTrue withCause(@Nullable IConstraint cause) {
        return new CTrue(cause);
    }

    @Override public CTrue apply(ISubstitution.Immutable subst) {
        return this;
    }

    @Override public Optional<ConstraintResult> solve(State state, ConstraintContext params) {
        return Optional.of(ConstraintResult.of(state));
    }
    
    @Override
    public Optional<MConstraintResult> solveMutable(MState state, MConstraintContext params) {
        return Optional.of(new MConstraintResult(state));
    }

    @Override public String toString(TermFormatter termToString) {
        return "true";
    }

    @Override public String toString() {
        return toString(ITerm::toString);
    }

}