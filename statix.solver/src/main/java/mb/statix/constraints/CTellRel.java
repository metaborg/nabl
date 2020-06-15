package mb.statix.constraints;

import java.io.Serializable;
import java.util.Objects;
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

public class CTellRel implements IConstraint, Serializable {
    private static final long serialVersionUID = 1L;

    private final ITerm scopeTerm;
    private final ITerm relation;
    private final ITerm datumTerm;

    private final @Nullable IConstraint cause;

    public CTellRel(ITerm scopeTerm, ITerm relation, ITerm datumTerm) {
        this(scopeTerm, relation, datumTerm, null);
    }

    public CTellRel(ITerm scopeTerm, ITerm relation, ITerm datumTerm, @Nullable IConstraint cause) {
        this.scopeTerm = scopeTerm;
        this.relation = relation;
        this.datumTerm = datumTerm;
        this.cause = cause;
    }

    public ITerm scopeTerm() {
        return scopeTerm;
    }

    public ITerm relation() {
        return relation;
    }

    public ITerm datumTerm() {
        return datumTerm;
    }

    @Override public Optional<IConstraint> cause() {
        return Optional.ofNullable(cause);
    }

    @Override public CTellRel withCause(@Nullable IConstraint cause) {
        return new CTellRel(scopeTerm, relation, datumTerm, cause);
    }

    @Override public <R> R match(Cases<R> cases) {
        return cases.caseTellRel(this);
    }

    @Override public <R, E extends Throwable> R matchOrThrow(CheckedCases<R, E> cases) throws E {
        return cases.caseTellRel(this);
    }

    @Override public Multiset<ITermVar> getVars() {
        final ImmutableMultiset.Builder<ITermVar> vars = ImmutableMultiset.builder();
        vars.addAll(scopeTerm.getVars());
        vars.addAll(datumTerm.getVars());
        return vars.build();
    }

    @Override public CTellRel apply(ISubstitution.Immutable subst) {
        return new CTellRel(subst.apply(scopeTerm), relation, subst.apply(datumTerm));
    }

    @Override public CTellRel apply(IRenaming subst) {
        return new CTellRel(subst.apply(scopeTerm), relation, subst.apply(datumTerm));
    }

    @Override public String toString(TermFormatter termToString) {
        final StringBuilder sb = new StringBuilder();
        sb.append(termToString.format(scopeTerm));
        sb.append(" -");
        sb.append(termToString.format(relation));
        sb.append("-[] ");
        sb.append(termToString.format(datumTerm));
        return sb.toString();
    }

    @Override public String toString() {
        return toString(ITerm::toString);
    }

    @Override
    public boolean equals(Object o) {
        if(this == o) return true;
        if(o == null || getClass() != o.getClass()) return false;
        CTellRel cTellRel = (CTellRel)o;
        return Objects.equals(scopeTerm, cTellRel.scopeTerm) &&
            Objects.equals(relation, cTellRel.relation) &&
            Objects.equals(datumTerm, cTellRel.datumTerm) &&
            Objects.equals(cause, cTellRel.cause);
    }

    @Override
    public int hashCode() {
        return Objects.hash(scopeTerm, relation, datumTerm, cause);
    }
}
