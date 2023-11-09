package mb.statix.constraints;

import java.io.Serializable;
import java.util.Objects;

import jakarta.annotation.Nullable;

import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.substitution.IRenaming;
import mb.nabl2.terms.substitution.ISubstitution;
import mb.nabl2.terms.substitution.ISubstitution.Immutable;
import mb.nabl2.util.TermFormatter;
import mb.scopegraph.oopsla20.reference.ResolutionException;
import mb.scopegraph.resolution.StateMachine;
import mb.statix.constraints.messages.IMessage;
import mb.statix.solver.IConstraint;
import mb.statix.solver.query.QueryFilter;
import mb.statix.solver.query.QueryMin;
import mb.statix.solver.query.QueryProject;

public final class CCompiledQuery extends AResolveQuery implements Serializable {

    private static final long serialVersionUID = 1L;

    private final StateMachine<ITerm> stateMachine;
    private final @Nullable CCompiledQuery origin;

    public CCompiledQuery(
            QueryFilter filter,
            QueryMin min,
            QueryProject project,
            ITerm scopeTerm,
            ITerm resultTerm,
            StateMachine<ITerm> stateMachine
    ) {
        this(filter, min, project, scopeTerm, resultTerm, null, null, null, stateMachine);
    }

    // Do not call this constructor. This is only used to reconstruct this object from a Statix term. Call withArguments() or withMessage() instead.
    public CCompiledQuery(
            QueryFilter filter,
            QueryMin min,
            QueryProject project,
            ITerm scopeTerm,
            ITerm resultTerm,
            @Nullable IMessage message,
            StateMachine<ITerm> stateMachine
    ) {
        this(filter, min, project, scopeTerm, resultTerm, null, message, null, stateMachine);
    }

    // Private constructor, so we can add more fields in the future. Externally call the appropriate with*() functions instead.
    private CCompiledQuery(
            QueryFilter filter,
            QueryMin min,
            QueryProject project,
            ITerm scopeTerm,
            ITerm resultTerm,
            @Nullable IConstraint cause,
            @Nullable IMessage message,
            @Nullable CCompiledQuery origin,
            StateMachine<ITerm> stateMachine
    ) {
        super(filter, min, project, scopeTerm, resultTerm, cause, message);
        this.origin = origin;
        this.stateMachine = stateMachine;
    }

    public CCompiledQuery withArguments(
            QueryFilter filter,
            QueryMin min,
            QueryProject project,
            ITerm scopeTerm,
            ITerm resultTerm,
            StateMachine<ITerm> stateMachine
    ) {
        if (this.filter == filter &&
            this.min == min &&
            this.project == project &&
            this.scopeTerm == scopeTerm &&
            this.resultTerm == resultTerm &&
            this.stateMachine == stateMachine
        ) {
            // Avoid creating new objects if the arguments are the exact same objects.
            // NOTE: Using `==` (instead of `Objects.equals()`) is cheap and already covers 99% of cases.
            return this;
        }
        return new CCompiledQuery(filter, min, project, scopeTerm, resultTerm, cause, message, origin, stateMachine);
    }

    @Override public @Nullable CCompiledQuery origin() {
        return origin;
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
        if (this.cause == cause) {
            // Avoid creating new objects if the arguments are the exact same objects.
            // NOTE: Using `==` (instead of `Objects.equals()`) is cheap and already covers 99% of cases.
            return this;
        }
        return new CCompiledQuery(
                filter,
                min,
                project,
                scopeTerm,
                resultTerm,
                cause,
                message,
                origin,
                stateMachine
        );
    }

    @Override public CCompiledQuery apply(ISubstitution.Immutable subst) {
        return apply(subst, false);
    }

    @Override public CCompiledQuery unsafeApply(ISubstitution.Immutable subst) {
        return unsafeApply(subst, false);
    }

    @Override public CCompiledQuery apply(IRenaming subst) {
        return apply(subst, false);
    }

    @Override public CCompiledQuery apply(ISubstitution.Immutable subst, boolean trackOrigin) {
        return new CCompiledQuery(
                filter.apply(subst, trackOrigin),
                min.apply(subst, trackOrigin),
                project,
                subst.apply(scopeTerm),
                subst.apply(resultTerm),
                cause,
                message == null ? null : message.apply(subst),
                origin == null && trackOrigin ? this : origin,
                stateMachine
        );
    }

    @Override public CCompiledQuery unsafeApply(ISubstitution.Immutable subst, boolean trackOrigin) {
        return new CCompiledQuery(
                filter.unsafeApply(subst, trackOrigin),
                min.unsafeApply(subst, trackOrigin),
                project,
                subst.apply(scopeTerm),
                subst.apply(resultTerm),
                cause,
                message == null ? null : message.apply(subst),
                origin == null && trackOrigin ? this : origin,
                stateMachine
        );
    }

    @Override public CCompiledQuery apply(IRenaming subst, boolean trackOrigin) {
        return new CCompiledQuery(
                filter.apply(subst, trackOrigin),
                min.apply(subst, trackOrigin),
                project,
                subst.apply(scopeTerm),
                subst.apply(resultTerm),
                cause,
                message == null ? null : message.apply(subst),
                origin == null && trackOrigin ? this : origin,
                stateMachine
        );
    }

    @Override public String toString(TermFormatter termToString) {
        final StringBuilder sb = new StringBuilder();
        sb.append("compiled query ");
        sb.append(filter.toString(termToString));
        sb.append(" ");
        sb.append(min.toString(termToString));
        sb.append(" ");
        sb.append(project.toString(termToString));
        sb.append(" in ");
        sb.append(termToString.format(scopeTerm));
        sb.append(" |-> ");
        sb.append(termToString.format(resultTerm));
        return sb.toString();
    }

    @Override public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        final CCompiledQuery that = (CCompiledQuery)o;
        // @formatter:off
        return this.hashCode == that.hashCode
            && Objects.equals(this.filter, that.filter)
            && Objects.equals(this.min, that.min)
            && Objects.equals(this.project, that.project)
            && Objects.equals(this.scopeTerm, that.scopeTerm)
            && Objects.equals(this.resultTerm, that.resultTerm)
            && Objects.equals(this.cause, that.cause)
            && Objects.equals(this.message, that.message)
            && Objects.equals(this.origin, that.origin)
            && Objects.equals(this.stateMachine, that.stateMachine);
        // @formatter:on
    }

    private final int hashCode = computeHashCode();

    @Override public int hashCode() {
        return hashCode;
    }

    private int computeHashCode() {
        return Objects.hash(
                filter,
                min,
                project,
                scopeTerm,
                resultTerm,
                cause,
                message,
                origin,
                stateMachine
        );
    }

}
