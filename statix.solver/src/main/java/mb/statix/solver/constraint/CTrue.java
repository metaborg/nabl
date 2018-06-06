package mb.statix.solver.constraint;

import java.util.Optional;

import org.metaborg.util.functions.Function1;

import com.google.common.collect.ImmutableSet;

import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.unification.IUnifier;
import mb.statix.solver.Completeness;
import mb.statix.solver.IConstraint;
import mb.statix.solver.IDebugContext;
import mb.statix.solver.Result;
import mb.statix.solver.State;

public class CTrue implements IConstraint {

    @Override public IConstraint apply(Function1<ITerm, ITerm> map) {
        return this;
    }

    @Override public Optional<Result> solve(State state, Completeness completeness, IDebugContext debug) {
        return Optional.of(Result.of(state, ImmutableSet.of()));
    }

    @Override public String toString(IUnifier unifier) {
        return toString();
    }

    @Override public String toString() {
        return "true";
    }

}