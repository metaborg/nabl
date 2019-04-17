package mb.statix.solver.constraint;

import java.io.Serializable;
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
 * Implementation for the true constraint.
 * 
 * <pre>true</pre>
 */
public class CTrue implements IConstraint, Serializable {
    private static final long serialVersionUID = 1L;

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

    @Override public <R> R match(Cases<R> cases) {
        return cases.caseTrue(this);
    }

    @Override public <R, E extends Throwable> R matchOrThrow(CheckedCases<R, E> cases) throws E {
        return cases.caseTrue(this);
    }

    @Override public CTrue apply(ISubstitution.Immutable subst) {
        return this;
    }

    @Override
    public Optional<MConstraintResult> solve(MState state, MConstraintContext params) {
        return Optional.of(new MConstraintResult());
    }

    @Override public String toString(TermFormatter termToString) {
        return "true";
    }

    @Override public String toString() {
        return toString(ITerm::toString);
    }

}