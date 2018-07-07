package mb.statix.solver.constraint;

import java.util.Optional;

import javax.annotation.Nullable;

import com.google.common.collect.ImmutableSet;

import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.substitution.ISubstitution;
import mb.nabl2.terms.unification.CannotUnifyException;
import mb.nabl2.terms.unification.IUnifier;
import mb.nabl2.terms.unification.OccursException;
import mb.nabl2.terms.unification.PersistentUnifier;
import mb.nabl2.terms.unification.RigidVarsException;
import mb.statix.solver.ConstraintContext;
import mb.statix.solver.Delay;
import mb.statix.solver.IConstraint;
import mb.statix.solver.Result;
import mb.statix.solver.State;

public class CEqual implements IConstraint {

    private final ITerm term1;
    private final ITerm term2;

    private final @Nullable IConstraint cause;

    public CEqual(ITerm term1, ITerm term2) {
        this(term1, term2, null);
    }

    public CEqual(ITerm term1, ITerm term2, @Nullable IConstraint cause) {
        this.term1 = term1;
        this.term2 = term2;
        this.cause = cause;
    }

    @Override public Optional<IConstraint> cause() {
        return Optional.ofNullable(cause);
    }

    @Override public CEqual withCause(@Nullable IConstraint cause) {
        return new CEqual(term1, term2, cause);
    }

    @Override public CEqual apply(ISubstitution.Immutable subst) {
        return new CEqual(subst.apply(term1), subst.apply(term2), cause);
    }

    @Override public Optional<Result> solve(State state, ConstraintContext params) throws Delay {
        IUnifier.Immutable unifier = state.unifier();
        try {
            final IUnifier.Immutable.Result<IUnifier.Immutable> result = unifier.unify(term1, term2, params::isRigid);
            params.debug().info("Unification succeeded: {}", result.result());
            final State newState = state.withUnifier(result.unifier());
            return Optional.of(Result.of(newState, ImmutableSet.of()));
        } catch(CannotUnifyException e) {
            params.debug().info("Unification failed: {} != {}", unifier.toString(e.getLeft()),
                    unifier.toString(e.getRight()));
            return Optional.empty();
        } catch(OccursException e) {
            params.debug().info("Unification failed: {} != {}", unifier.toString(term1), unifier.toString(term2));
            return Optional.empty();
        } catch(RigidVarsException e) {
            throw Delay.ofVars(e.vars());
        }
    }

    @Override public String toString(IUnifier unifier) {
        final StringBuilder sb = new StringBuilder();
        sb.append(unifier.toString(term1));
        sb.append(" == ");
        sb.append(unifier.toString(term2));
        return sb.toString();
    }

    @Override public String toString() {
        return toString(PersistentUnifier.Immutable.of());
    }

}