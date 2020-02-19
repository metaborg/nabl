package mb.statix.constraints;

import java.io.Serializable;
import java.util.Optional;
import java.util.Set;

import javax.annotation.Nullable;

import com.google.common.collect.ImmutableSet;

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

    @Override public Set<ITermVar> boundVars() {
        return ImmutableSet.of();
    }

    @Override public Set<ITermVar> freeVars() {
        final ImmutableSet.Builder<ITermVar> freeVars = ImmutableSet.builder();
        freeVars.addAll(filter.freeVars());
        freeVars.addAll(min.freeVars());
        freeVars.addAll(scopeTerm.getVars());
        freeVars.addAll(resultTerm.getVars());
        message().ifPresent(m -> freeVars.addAll(m.freeVars()));
        return freeVars.build();
    }

    @Override public IConstraint doSubstitute(IRenaming.Immutable localRenaming, ISubstitution.Immutable totalSubst) {
        return new CResolveQuery(relation, filter.recSubstitute(totalSubst), min.recSubstitute(totalSubst),
                totalSubst.apply(scopeTerm), totalSubst.apply(resultTerm), cause,
                message == null ? null : message.recSubstitute(totalSubst));
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

}
