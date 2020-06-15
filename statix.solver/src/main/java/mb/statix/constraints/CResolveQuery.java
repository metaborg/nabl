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
import mb.statix.constraints.messages.IMessage;
import mb.statix.solver.IConstraint;
import mb.statix.solver.query.IQueryFilter;
import mb.statix.solver.query.IQueryMin;

public class CResolveQuery implements IConstraint, Serializable {
    private static final long serialVersionUID = 1L;

    private final ITerm relation;
    private final IQueryFilter filter;
    private final IQueryMin min;
    private final ITerm scopeTerm;
    private final ITerm resultTerm;

    private final @Nullable IConstraint cause;
    private final @Nullable IMessage message;

    public CResolveQuery(ITerm relation, IQueryFilter filter, IQueryMin min, ITerm scopeTerm, ITerm resultTerm) {
        this(relation, filter, min, scopeTerm, resultTerm, null, null);
    }

    public CResolveQuery(ITerm relation, IQueryFilter filter, IQueryMin min, ITerm scopeTerm, ITerm resultTerm,
            @Nullable IMessage message) {
        this(relation, filter, min, scopeTerm, resultTerm, null, message);
    }

    public CResolveQuery(ITerm relation, IQueryFilter filter, IQueryMin min, ITerm scopeTerm, ITerm resultTerm,
            @Nullable IConstraint cause, @Nullable IMessage message) {
        this.relation = relation;
        this.filter = filter;
        this.min = min;
        this.scopeTerm = scopeTerm;
        this.resultTerm = resultTerm;
        this.cause = cause;
        this.message = message;
    }

    public ITerm relation() {
        return relation;
    }

    public IQueryFilter filter() {
        return filter;
    }

    public IQueryMin min() {
        return min;
    }

    public ITerm scopeTerm() {
        return scopeTerm;
    }

    public ITerm resultTerm() {
        return resultTerm;
    }

    @Override public Optional<IConstraint> cause() {
        return Optional.ofNullable(cause);
    }

    @Override public CResolveQuery withCause(@Nullable IConstraint cause) {
        return new CResolveQuery(relation, filter, min, scopeTerm, resultTerm, cause, message);
    }

    @Override public Optional<IMessage> message() {
        return Optional.ofNullable(message);
    }

    @Override public CResolveQuery withMessage(@Nullable IMessage message) {
        return new CResolveQuery(relation, filter, min, scopeTerm, resultTerm, cause, message);
    }

    @Override public <R> R match(Cases<R> cases) {
        return cases.caseResolveQuery(this);
    }

    @Override public <R, E extends Throwable> R matchOrThrow(CheckedCases<R, E> cases) throws E {
        return cases.caseResolveQuery(this);
    }

    @Override public Multiset<ITermVar> getVars() {
        final ImmutableMultiset.Builder<ITermVar> vars = ImmutableMultiset.builder();
        vars.addAll(filter.getVars());
        vars.addAll(min.getVars());
        vars.addAll(scopeTerm.getVars());
        vars.addAll(resultTerm.getVars());
        return vars.build();
    }

    @Override public CResolveQuery apply(ISubstitution.Immutable subst) {
        return new CResolveQuery(relation, filter.apply(subst), min.apply(subst), subst.apply(scopeTerm),
                subst.apply(resultTerm), cause, message == null ? null : message.apply(subst));
    }

    @Override public CResolveQuery apply(IRenaming subst) {
        return new CResolveQuery(relation, filter.apply(subst), min.apply(subst), subst.apply(scopeTerm),
                subst.apply(resultTerm), cause, message == null ? null : message.apply(subst));
    }

    @Override public String toString(TermFormatter termToString) {
        final StringBuilder sb = new StringBuilder();
        sb.append("query ");
        sb.append(relation);
        sb.append(" ");
        sb.append(filter.toString(termToString));
        sb.append(" ");
        sb.append(min.toString(termToString));
        sb.append(" in ");
        sb.append(termToString.format(scopeTerm));
        sb.append(" |-> ");
        sb.append(termToString.format(resultTerm));
        return sb.toString();
    }

    @Override public String toString() {
        return toString(ITerm::toString);
    }

    @Override
    public boolean equals(Object o) {
        if(this == o) return true;
        if(o == null || getClass() != o.getClass()) return false;
        CResolveQuery that = (CResolveQuery)o;
        return Objects.equals(relation, that.relation) &&
            Objects.equals(filter, that.filter) &&
            Objects.equals(min, that.min) &&
            Objects.equals(scopeTerm, that.scopeTerm) &&
            Objects.equals(resultTerm, that.resultTerm) &&
            Objects.equals(cause, that.cause) &&
            Objects.equals(message, that.message);
    }

    @Override
    public int hashCode() {
        return Objects.hash(relation, filter, min, scopeTerm, resultTerm, cause, message);
    }
}
