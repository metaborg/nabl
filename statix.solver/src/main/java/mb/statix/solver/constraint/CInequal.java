package mb.statix.solver.constraint;

import java.util.Optional;

import javax.annotation.Nullable;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;

import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.substitution.ISubstitution;
import mb.nabl2.terms.unification.IUnifier;
import mb.nabl2.terms.unification.PersistentUnifier;
import mb.statix.solver.ConstraintContext;
import mb.statix.solver.Delay;
import mb.statix.solver.IConstraint;
import mb.statix.solver.Result;
import mb.statix.solver.State;

public class CInequal implements IConstraint {

    private final ITerm term1;
    private final ITerm term2;

    private final @Nullable IConstraint cause;

    public CInequal(ITerm term1, ITerm term2) {
        this(term1, term2, null);
    }

    public CInequal(ITerm term1, ITerm term2, @Nullable IConstraint cause) {
        this.term1 = term1;
        this.term2 = term2;
        this.cause = cause;
    }

    @Override public Optional<IConstraint> cause() {
        return Optional.ofNullable(cause);
    }

    @Override public CInequal withCause(@Nullable IConstraint cause) {
        return new CInequal(term1, term2, cause);
    }

    @Override public CInequal apply(ISubstitution.Immutable subst) {
        return new CInequal(subst.apply(term1), subst.apply(term2), cause);
    }

    @Override public Optional<Result> solve(State state, ConstraintContext params) throws Delay {
        IUnifier.Immutable unifier = state.unifier();
        if(unifier.areUnequal(term1, term2)) {
            return Optional.of(Result.of(state, ImmutableSet.of()));
        } else if(unifier.areEqual(term1, term2)) {
            return Optional.empty();
        } else {
            throw Delay.ofVars(Iterables.concat(unifier.getVars(term1), unifier.getVars(term2)));
        }
    }

    @Override public String toString(IUnifier unifier) {
        final StringBuilder sb = new StringBuilder();
        sb.append(unifier.toString(term1));
        sb.append(" != ");
        sb.append(unifier.toString(term2));
        return sb.toString();
    }

    @Override public String toString() {
        return toString(PersistentUnifier.Immutable.of());
    }

}