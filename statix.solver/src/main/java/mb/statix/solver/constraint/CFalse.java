package mb.statix.solver.constraint;

import java.util.Optional;

import com.google.common.collect.ImmutableSet;

import mb.nabl2.terms.substitution.ISubstitution;
import mb.nabl2.terms.unification.IUnifier;
import mb.nabl2.terms.unification.PersistentUnifier;
import mb.statix.solver.Completeness;
import mb.statix.solver.IConstraint;
import mb.statix.solver.IDebugContext;
import mb.statix.solver.Result;
import mb.statix.solver.State;

public class CFalse implements IConstraint {

    @Override public IConstraint apply(ISubstitution.Immutable subst) {
        return this;
    }

    @Override public Optional<Result> solve(final State state, Completeness completeness, IDebugContext debug) {
        final State newState = state.withErroneous(true);
        return Optional.of(Result.of(newState, ImmutableSet.of()));
    }

    @Override public String toString(IUnifier unifier) {
        return "false";
    }

    @Override public String toString() {
        return toString(PersistentUnifier.Immutable.of());
    }

}