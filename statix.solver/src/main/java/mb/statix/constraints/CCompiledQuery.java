package mb.statix.constraints;

import java.io.Serializable;
import java.util.Objects;

import javax.annotation.Nullable;

import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.substitution.IRenaming;
import mb.nabl2.terms.substitution.ISubstitution.Immutable;
import mb.nabl2.util.TermFormatter;
import mb.scopegraph.oopsla20.reference.ResolutionException;
import mb.scopegraph.resolution.StateMachine;
import mb.statix.constraints.messages.IMessage;
import mb.statix.solver.IConstraint;
import mb.statix.solver.query.QueryFilter;
import mb.statix.solver.query.QueryMin;

public class CCompiledQuery extends AResolveQuery implements Serializable {

    private static final long serialVersionUID = 1L;

    private final StateMachine<ITerm> stateMachine;

    public CCompiledQuery(QueryFilter filter, QueryMin min, ITerm scopeTerm, ITerm resultTerm,
            StateMachine<ITerm> stateMachine) {
        this(filter, min, scopeTerm, resultTerm, null, null, stateMachine);
    }

    public CCompiledQuery(QueryFilter filter, QueryMin min, ITerm scopeTerm, ITerm resultTerm,
            @Nullable IMessage message, StateMachine<ITerm> stateMachine) {
        this(filter, min, scopeTerm, resultTerm, null, message, stateMachine);
    }

    public CCompiledQuery(QueryFilter filter, QueryMin min, ITerm scopeTerm, ITerm resultTerm, IConstraint cause,
            IMessage message, StateMachine<ITerm> stateMachine) {
        super(filter, min, scopeTerm, resultTerm, cause, message);
        this.stateMachine = stateMachine;
    }

    public StateMachine<ITerm> stateMachine() {
        return stateMachine;
    }

    @Override public <R> R match(Cases<R> cases) {
        return cases.caseCompiledQuery(this);
    }

    @Override public <R, E extends Throwable> R matchInResolution(ResolutionFunction1<CResolveQuery, R> onResolveQuery,
            ResolutionFunction1<CCompiledQuery, R> onCompiledQuery) throws ResolutionException, InterruptedException {
        return onCompiledQuery.apply(this);
    }

    @Override public CCompiledQuery withCause(IConstraint cause) {
        return new CCompiledQuery(filter, min, scopeTerm, resultTerm, cause, message, stateMachine);
    }

    @Override public CCompiledQuery apply(Immutable subst) {
        return new CCompiledQuery(filter.apply(subst), min.apply(subst), subst.apply(scopeTerm),
                subst.apply(resultTerm), cause, message == null ? null : message.apply(subst), stateMachine);
    }

    @Override public CCompiledQuery unsafeApply(Immutable subst) {
        return new CCompiledQuery(filter.unsafeApply(subst), min.unsafeApply(subst), subst.apply(scopeTerm),
                subst.apply(resultTerm), cause, message == null ? null : message.apply(subst), stateMachine);
    }

    @Override public CCompiledQuery apply(IRenaming subst) {
        return new CCompiledQuery(filter.apply(subst), min.apply(subst), subst.apply(scopeTerm),
                subst.apply(resultTerm), cause, message == null ? null : message.apply(subst), stateMachine);
    }

    @Override public String toString(TermFormatter termToString) {
        final StringBuilder sb = new StringBuilder();
        sb.append("compiled query ");
        sb.append(filter.toString(termToString));
        sb.append(" ");
        sb.append(min.toString(termToString));
        sb.append(" in ");
        sb.append(termToString.format(scopeTerm));
        sb.append(" |-> ");
        sb.append(termToString.format(resultTerm));
        return sb.toString();
    }

    @Override public boolean equals(Object o) {
        if(this == o)
            return true;
        if(o == null || getClass() != o.getClass())
            return false;
        CCompiledQuery that = (CCompiledQuery) o;
        return Objects.equals(filter, that.filter) && Objects.equals(min, that.min)
                && Objects.equals(scopeTerm, that.scopeTerm) && Objects.equals(resultTerm, that.resultTerm)
                && Objects.equals(cause, that.cause) && Objects.equals(message, that.message)
                && Objects.equals(stateMachine, that.stateMachine);
    }

    private volatile int hashCode;

    @Override public int hashCode() {
        int result = hashCode;
        if(result == 0) {
            result = Objects.hash(filter, min, scopeTerm, resultTerm, cause, message, stateMachine);
            hashCode = result;
        }
        return result;
    }

}
