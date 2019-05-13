package mb.statix.solver.constraint;

import static mb.nabl2.terms.build.TermBuild.B;

import java.io.Serializable;
import java.util.List;
import java.util.Optional;

import javax.annotation.Nullable;

import com.google.common.collect.ImmutableList;

import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.substitution.ISubstitution;
import mb.nabl2.util.TermFormatter;
import mb.statix.solver.IConstraint;

public class CTellRel implements IConstraint, Serializable {
    private static final long serialVersionUID = 1L;

    private final ITerm scopeTerm;
    private final ITerm relation;
    private final List<ITerm> datumTerms;

    private final @Nullable IConstraint cause;

    public CTellRel(ITerm scopeTerm, ITerm relation, Iterable<ITerm> datumTerms) {
        this(scopeTerm, relation, datumTerms, null);
    }

    public CTellRel(ITerm scopeTerm, ITerm relation, Iterable<ITerm> datumTerms, @Nullable IConstraint cause) {
        this.scopeTerm = scopeTerm;
        this.relation = relation;
        this.datumTerms = ImmutableList.copyOf(datumTerms);
        this.cause = cause;
    }

    public ITerm scopeTerm() {
        return scopeTerm;
    }

    public ITerm relation() {
        return relation;
    }

    public List<ITerm> datumTerms() {
        return datumTerms;
    }

    public ITerm datumTerm() {
        return B.newTuple(datumTerms);
    }

    @Override public Optional<IConstraint> cause() {
        return Optional.ofNullable(cause);
    }

    @Override public CTellRel withCause(@Nullable IConstraint cause) {
        return new CTellRel(scopeTerm, relation, datumTerms, cause);
    }

    @Override public <R> R match(Cases<R> cases) {
        return cases.caseTellRel(this);
    }

    @Override public <R, E extends Throwable> R matchOrThrow(CheckedCases<R, E> cases) throws E {
        return cases.caseTellRel(this);
    }

    @Override public CTellRel apply(ISubstitution.Immutable subst) {
        return new CTellRel(subst.apply(scopeTerm), relation, subst.apply(datumTerms));
    }

    @Override public String toString(TermFormatter termToString) {
        final StringBuilder sb = new StringBuilder();
        sb.append(termToString.format(scopeTerm));
        sb.append(" -");
        sb.append(termToString.format(relation));
        sb.append("-[] ");
        sb.append(termToString.format(B.newTuple(datumTerms)));
        return sb.toString();
    }

    @Override public String toString() {
        return toString(ITerm::toString);
    }

}