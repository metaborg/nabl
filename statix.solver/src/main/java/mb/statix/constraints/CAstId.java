package mb.statix.constraints;

import java.io.Serializable;
import java.util.Optional;

import javax.annotation.Nullable;

import com.google.common.collect.ImmutableMultiset;
import com.google.common.collect.Multiset;
import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.ITermVar;
import mb.nabl2.terms.substitution.IRenaming;
import mb.nabl2.terms.substitution.ISubstitution;
import mb.nabl2.util.TermFormatter;
import mb.statix.solver.IConstraint;

public class CAstId implements IConstraint, Serializable {
    private static final long serialVersionUID = 1L;

    private final ITerm term;
    private final ITerm idTerm;

    private final @Nullable IConstraint cause;

    public CAstId(ITerm term, ITerm idTerm) {
        this(term, idTerm, null);
    }

    public CAstId(ITerm term, ITerm idTerm, @Nullable IConstraint cause) {
        this.term = term;
        this.idTerm = idTerm;
        this.cause = cause;
    }

    public ITerm astTerm() {
        return term;
    }

    public ITerm idTerm() {
        return idTerm;
    }

    @Override public Optional<IConstraint> cause() {
        return Optional.ofNullable(cause);
    }

    @Override public CAstId withCause(@Nullable IConstraint cause) {
        return new CAstId(term, idTerm, cause);
    }

    @Override public <R> R match(Cases<R> cases) {
        return cases.caseTermId(this);
    }

    @Override public <R, E extends Throwable> R matchOrThrow(CheckedCases<R, E> cases) throws E {
        return cases.caseTermId(this);
    }

    @Override public Multiset<ITermVar> getVars() {
        final ImmutableMultiset.Builder<ITermVar> vars = ImmutableMultiset.builder();
        vars.addAll(term.getVars());
        vars.addAll(idTerm.getVars());
        return vars.build();
    }

    @Override public CAstId apply(ISubstitution.Immutable subst) {
        return new CAstId(subst.apply(term), subst.apply(idTerm), cause);
    }

    @Override public CAstId apply(IRenaming subst) {
        return new CAstId(subst.apply(term), subst.apply(idTerm), cause);
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
