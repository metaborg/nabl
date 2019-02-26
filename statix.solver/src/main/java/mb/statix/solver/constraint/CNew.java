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
import mb.nabl2.util.TermFormatter;
import mb.nabl2.util.Tuple2;
import mb.statix.solver.ConstraintContext;
import mb.statix.solver.ConstraintResult;
import mb.statix.solver.IConstraint;
import mb.statix.solver.State;
import mb.statix.taico.solver.MConstraintResult;
import mb.statix.taico.solver.MState;

/**
 * Implementation for the new (scope) constraint.
 * 
 * <pre>new scopes</pre>
 */
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

    @Override public Optional<ConstraintResult> solve(State state, ConstraintContext params) {
        final List<IConstraint> constraints = Lists.newArrayList();
        State newState = state;
        for(ITerm t : terms) {
            final String base = M.var(ITermVar::getName).match(t).orElse("s");
            Tuple2<ITerm, State> ss = newState.freshScope(base);
            constraints.add(new CEqual(t, ss._1(), this));
            newState = ss._2();
        }
        return Optional.of(ConstraintResult.ofConstraints(newState, constraints));
    }
    
    @Override public Optional<MConstraintResult> solveMutable(MState state, ConstraintContext params) {
        final List<IConstraint> constraints = Lists.newArrayList();
        for(ITerm t : terms) {
            final String base = M.var(ITermVar::getName).match(t).orElse("s");
            ITerm ss = state.freshScope(base);
            constraints.add(new CEqual(t, ss, this));
        }
        return Optional.of(MConstraintResult.ofConstraints(state, constraints));
    }

    @Override public String toString(TermFormatter termToString) {
        final StringBuilder sb = new StringBuilder();
        sb.append("new ");
        sb.append(termToString.format(terms));
        return sb.toString();
    }

    @Override public String toString() {
        return toString(ITerm::toString);
    }

}