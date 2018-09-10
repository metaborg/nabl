package mb.statix.solver.constraint;

import java.util.Optional;

import javax.annotation.Nullable;

import mb.nabl2.terms.substitution.ISubstitution;
import mb.nabl2.terms.unification.IUnifier;
import mb.nabl2.terms.unification.PersistentUnifier;
import mb.statix.solver.ConstraintContext;
import mb.statix.solver.ConstraintResult;
import mb.statix.solver.Delay;
import mb.statix.solver.IConstraint;
import mb.statix.solver.State;

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

    @Override public Optional<ConstraintResult> solve(final State state, ConstraintContext params)
            throws Delay {
        return Optional.empty();
    }

    @Override public String toString(IUnifier unifier) {
        return "false";
    }

    @Override public String toString() {
        return toString(PersistentUnifier.Immutable.of());
    }

}