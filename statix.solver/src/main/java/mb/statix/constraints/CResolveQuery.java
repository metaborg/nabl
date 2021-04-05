package mb.statix.constraints;

import java.io.Serializable;
import java.util.Objects;
import java.util.Optional;

import javax.annotation.Nullable;

import org.metaborg.util.functions.Action1;

import io.usethesource.capsule.Set;
import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.ITermVar;
import mb.nabl2.terms.substitution.IRenaming;
import mb.nabl2.terms.substitution.ISubstitution;
import mb.nabl2.util.CapsuleUtil;
import mb.nabl2.util.TermFormatter;
import mb.statix.constraints.messages.IMessage;
import mb.statix.solver.IConstraint;
import mb.statix.solver.query.QueryFilter;
import mb.statix.solver.query.QueryMin;

public class CResolveQuery implements IConstraint, Serializable {
    private static final long serialVersionUID = 1L;

    private final QueryFilter filter;
    private final QueryMin min;
    private final ITerm scopeTerm;
    private final ITerm resultTerm;

    private final @Nullable IConstraint cause;
    private final @Nullable IMessage message;

    public CResolveQuery(QueryFilter filter, QueryMin min, ITerm scopeTerm, ITerm resultTerm) {
        this(filter, min, scopeTerm, resultTerm, null, null);
    }

    public CResolveQuery(QueryFilter filter, QueryMin min, ITerm scopeTerm, ITerm resultTerm,
            @Nullable IMessage message) {
        this(filter, min, scopeTerm, resultTerm, null, message);
    }

    public CResolveQuery(QueryFilter filter, QueryMin min, ITerm scopeTerm, ITerm resultTerm,
            @Nullable IConstraint cause, @Nullable IMessage message) {
        this.filter = filter;
        this.min = min;
        this.scopeTerm = scopeTerm;
        this.resultTerm = resultTerm;
        this.cause = cause;
        this.message = message;
    }

    public QueryFilter filter() {
        return filter;
    }

    public QueryMin min() {
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
        return new CResolveQuery(filter, min, scopeTerm, resultTerm, cause, message);
    }

    @Override public Optional<IMessage> message() {
        return Optional.ofNullable(message);
    }

    @Override public CResolveQuery withMessage(@Nullable IMessage message) {
        return new CResolveQuery(filter, min, scopeTerm, resultTerm, cause, message);
    }

    @Override public <R> R match(Cases<R> cases) {
        return cases.caseResolveQuery(this);
    }

    @Override public <R, E extends Throwable> R matchOrThrow(CheckedCases<R, E> cases) throws E {
        return cases.caseResolveQuery(this);
    }

    @Override public Set.Immutable<ITermVar> freeVars() {
        Set.Transient<ITermVar> freeVars = CapsuleUtil.transientSet();
        doVisitFreeVars(freeVars::__insert);
        return freeVars.freeze();
    }

    @Override public void visitFreeVars(Action1<ITermVar> onFreeVar) {
        doVisitFreeVars(onFreeVar);
    }

    private void doVisitFreeVars(Action1<ITermVar> onFreeVar) {
        scopeTerm.getVars().forEach(onFreeVar::apply);
        filter.getDataWF().visitFreeVars(onFreeVar);
        min.getDataEquiv().visitFreeVars(onFreeVar);
        resultTerm.getVars().forEach(onFreeVar::apply);
        if(message != null) {
            message.visitVars(onFreeVar);
        }

    }

    @Override public CResolveQuery apply(ISubstitution.Immutable subst) {
        return new CResolveQuery(filter.apply(subst), min.apply(subst), subst.apply(scopeTerm), subst.apply(resultTerm),
                cause, message == null ? null : message.apply(subst));
    }

    @Override public CResolveQuery unsafeApply(ISubstitution.Immutable subst) {
        return new CResolveQuery(filter.unsafeApply(subst), min.unsafeApply(subst), subst.apply(scopeTerm),
                subst.apply(resultTerm), cause, message == null ? null : message.apply(subst));
    }

    @Override public CResolveQuery apply(IRenaming subst) {
        return new CResolveQuery(filter.apply(subst), min.apply(subst), subst.apply(scopeTerm), subst.apply(resultTerm),
                cause, message == null ? null : message.apply(subst));
    }

    @Override public String toString(TermFormatter termToString) {
        final StringBuilder sb = new StringBuilder();
        sb.append("query ");
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

    @Override public boolean equals(Object o) {
        if(this == o)
            return true;
        if(o == null || getClass() != o.getClass())
            return false;
        CResolveQuery that = (CResolveQuery) o;
        return Objects.equals(filter, that.filter) && Objects.equals(min, that.min)
                && Objects.equals(scopeTerm, that.scopeTerm) && Objects.equals(resultTerm, that.resultTerm)
                && Objects.equals(cause, that.cause) && Objects.equals(message, that.message);
    }

    private volatile int hashCode;

    @Override public int hashCode() {
        int result = hashCode;
        if(result == 0) {
            result = Objects.hash(filter, min, scopeTerm, resultTerm, cause, message);
            hashCode = result;
        }
        return result;
    }

}
