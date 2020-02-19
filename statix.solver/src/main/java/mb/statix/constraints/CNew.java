package mb.statix.constraints;

import java.io.Serializable;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import javax.annotation.Nullable;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.ITermVar;
import mb.nabl2.terms.substitution.IRenaming;
import mb.nabl2.terms.substitution.ISubstitution;
import mb.nabl2.util.TermFormatter;
import mb.statix.solver.IConstraint;

public class CNew implements IConstraint, Serializable {
    private static final long serialVersionUID = 1L;

    private final List<ITerm> terms;

    private final @Nullable IConstraint cause;

    public CNew(Iterable<ITerm> terms) {
        this(terms, null);
    }

    public CNew(Iterable<ITerm> terms, @Nullable IConstraint cause) {
        this.terms = ImmutableList.copyOf(terms);
        this.cause = cause;
    }

    public List<ITerm> terms() {
        return terms;
    }

    @Override public <R> R match(Cases<R> cases) {
        return cases.caseNew(this);
    }

    @Override public <R, E extends Throwable> R matchOrThrow(CheckedCases<R, E> cases) throws E {
        return cases.caseNew(this);
    }

    @Override public Optional<IConstraint> cause() {
        return Optional.ofNullable(cause);
    }

    @Override public CNew withCause(@Nullable IConstraint cause) {
        return new CNew(terms, cause);
    }

    @Override public Set<ITermVar> boundVars() {
        return ImmutableSet.of();
    }

    @Override public Set<ITermVar> freeVars() {
        final ImmutableSet.Builder<ITermVar> freeVars = ImmutableSet.builder();
        terms.forEach(t -> freeVars.addAll(t.getVars()));
        return freeVars.build();
    }

    @Override public CNew doSubstitute(IRenaming.Immutable localRenaming, ISubstitution.Immutable totalSubst) {
        return new CNew(totalSubst.applyTerms(terms), cause);
    }

    @Override public String toString(TermFormatter termToString) {
        final StringBuilder sb = new StringBuilder();
        sb.append("new ");
        sb.append(termToString.format(terms, " "));
        return sb.toString();
    }

    @Override public String toString() {
        return toString(ITerm::toString);
    }

}