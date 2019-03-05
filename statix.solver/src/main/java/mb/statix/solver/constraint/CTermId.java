package mb.statix.solver.constraint;

import java.util.Optional;

import javax.annotation.Nullable;

import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.substitution.ISubstitution;
import mb.nabl2.util.TermFormatter;
import mb.statix.solver.IConstraint;

public class CTermId implements IConstraint {

    private final ITerm term;
    private final ITerm idTerm;

    private final @Nullable IConstraint cause;

    public CTermId(ITerm term, ITerm idTerm) {
        this(term, idTerm, null);
    }

    public CTermId(ITerm term, ITerm idTerm, @Nullable IConstraint cause) {
        this.term = term;
        this.idTerm = idTerm;
        this.cause = cause;
    }

    public ITerm term() {
        return term;
    }

    public ITerm idTerm() {
        return idTerm;
    }

    @Override public Optional<IConstraint> cause() {
        return Optional.ofNullable(cause);
    }

    @Override public CTermId withCause(@Nullable IConstraint cause) {
        return new CTermId(term, idTerm, cause);
    }

    @Override public <R> R match(Cases<R> cases) {
        return cases.caseTermId(this);
    }

    @Override public <R, E extends Throwable> R matchOrThrow(CheckedCases<R, E> cases) throws E {
        return cases.caseTermId(this);
    }

    @Override public CTermId apply(ISubstitution.Immutable subst) {
        return new CTermId(subst.apply(term), subst.apply(idTerm), cause);
    }

    @Override public String toString(TermFormatter termToString) {
        final StringBuilder sb = new StringBuilder();
        sb.append("termId(");
        sb.append(termToString.format(term));
        sb.append(", ");
        sb.append(termToString.format(idTerm));
        sb.append(")");
        return sb.toString();
    }

    @Override public String toString() {
        return toString(ITerm::toString);
    }

}