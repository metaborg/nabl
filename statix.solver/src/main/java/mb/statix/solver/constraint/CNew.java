package mb.statix.solver.constraint;

import static mb.nabl2.terms.matching.TermMatch.M;

import java.util.List;
import java.util.Optional;

import javax.annotation.Nullable;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.ITermVar;
import mb.nabl2.terms.substitution.ISubstitution;
import mb.nabl2.terms.unification.IUnifier;
import mb.nabl2.terms.unification.PersistentUnifier;
import mb.nabl2.util.Tuple2;
import mb.statix.solver.Completeness;
import mb.statix.solver.Delay;
import mb.statix.solver.IConstraint;
import mb.statix.solver.IDebugContext;
import mb.statix.solver.Result;
import mb.statix.solver.State;

public class CNew implements IConstraint {

    private final List<ITerm> terms;

    private final @Nullable IConstraint cause;

    public CNew(Iterable<ITerm> terms) {
        this(terms, null);
    }

    public CNew(Iterable<ITerm> terms, @Nullable IConstraint cause) {
        this.terms = ImmutableList.copyOf(terms);
        this.cause = cause;
    }

    @Override public Optional<IConstraint> cause() {
        return Optional.ofNullable(cause);
    }

    @Override public CNew withCause(@Nullable IConstraint cause) {
        return new CNew(terms, cause);
    }

    @Override public CNew apply(ISubstitution.Immutable subst) {
        return new CNew(subst.apply(terms), cause);
    }

    @Override public Optional<Result> solve(State state, Completeness completeness, IDebugContext debug) throws Delay {
        final List<IConstraint> constraints = Lists.newArrayList();
        State newState = state;
        for(ITerm t : terms) {
            final String base = M.var(ITermVar::getName).match(t).orElse("s");
            Tuple2<ITerm, State> ss = newState.freshScope(base);
            constraints.add(new CEqual(t, ss._1(), this));
            newState = ss._2();
        }
        return Optional.of(Result.of(newState, constraints));
    }

    @Override public String toString(IUnifier unifier) {
        final StringBuilder sb = new StringBuilder();
        sb.append("new ");
        sb.append(unifier.toString(terms));
        return sb.toString();
    }

    @Override public String toString() {
        return toString(PersistentUnifier.Immutable.of());
    }

}