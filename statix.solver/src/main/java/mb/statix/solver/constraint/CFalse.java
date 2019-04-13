package mb.statix.solver.constraint;

import java.util.Optional;

import javax.annotation.Nullable;

import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.substitution.ISubstitution;
import mb.nabl2.util.TermFormatter;
import mb.statix.solver.IConstraint;
import mb.statix.taico.solver.MConstraintContext;
import mb.statix.taico.solver.MConstraintResult;
import mb.statix.taico.solver.MState;

/**
 * Implementation for the {@code false} constraint.
 * 
 * <pre>false</pre>
 */
public class CFalse implements IConstraint {

    private final @Nullable IConstraint cause;

    public CFalse() {
        this(null);
    }

    public CFalse(@Nullable IConstraint cause) {
        this.cause = cause;
    }

    @Override public Optional<IConstraint> cause() {
        return Optional.ofNullable(cause);
    }

    @Override public CFalse withCause(@Nullable IConstraint cause) {
        return new CFalse(cause);
    }

    @Override public CFalse apply(ISubstitution.Immutable subst) {
        return this;
    }

    /**
     * @see IConstraint#solve
     * 
     * @return
     *      an empty optional
     */
    @Override
    public Optional<MConstraintResult> solve(MState state, MConstraintContext params) {
        return Optional.empty();
    }

    @Override public String toString(TermFormatter termToString) {
        return "false";
    }

    @Override public String toString() {
        return toString(ITerm::toString);
    }

}