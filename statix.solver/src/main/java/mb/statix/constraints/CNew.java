package mb.statix.constraints;

import java.io.Serializable;
import java.util.Objects;
import java.util.Optional;

import javax.annotation.Nullable;

import org.metaborg.util.collection.CapsuleUtil;
import org.metaborg.util.functions.Action1;

import io.usethesource.capsule.Set;
import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.ITermVar;
import mb.nabl2.terms.substitution.IRenaming;
import mb.nabl2.terms.substitution.ISubstitution;
import mb.nabl2.util.TermFormatter;
import mb.statix.solver.IConstraint;
import mb.statix.solver.completeness.ICompleteness;

public final class CNew implements IConstraint, Serializable {
    private static final long serialVersionUID = 1L;

    private final ITerm scopeTerm;
    private final ITerm datumTerm;

    private final @Nullable IConstraint cause;
    private final @Nullable ICompleteness.Immutable ownCriticalEdges;

    public CNew(ITerm scopeTerm, ITerm datumTerm) {
        this(scopeTerm, datumTerm, null, null);
    }

    // Private constructor, so we can add more fields in the future. Externally call the appropriate with*() functions instead.
    private CNew(
            ITerm scopeTerm,
            ITerm datumTerm,
            @Nullable IConstraint cause,
            @Nullable ICompleteness.Immutable ownCriticalEdges
    ) {
        this.scopeTerm = scopeTerm;
        this.datumTerm = datumTerm;
        this.cause = cause;
        this.ownCriticalEdges = ownCriticalEdges;
    }

    public ITerm scopeTerm() {
        return scopeTerm;
    }

    public ITerm datumTerm() {
        return datumTerm;
    }

    public CNew withArguments(ITerm scopeTerm, ITerm datumTerm) {
        return new CNew(scopeTerm, datumTerm, cause, ownCriticalEdges);
    }

    @Override public <R> R match(Cases<R> cases) {
        return cases.caseNew(this);
    }

    @Override public <R, E extends Throwable> R matchOrThrow(CheckedCases<R, E> cases) throws E {
        return cases.caseNew(this);
    }

    @Override public Set.Immutable<ITermVar> getVars() {
        return Set.Immutable.union(
                scopeTerm.getVars(),
                datumTerm.getVars()
        );
    }

    @Override public Optional<IConstraint> cause() {
        return Optional.ofNullable(cause);
    }

    @Override public CNew withCause(@Nullable IConstraint cause) {
        return new CNew(scopeTerm, datumTerm, cause, ownCriticalEdges);
    }

    @Override public Optional<ICompleteness.Immutable> ownCriticalEdges() {
        return Optional.ofNullable(ownCriticalEdges);
    }

    @Override public CNew withOwnCriticalEdges(ICompleteness.Immutable criticalEdges) {
        return new CNew(scopeTerm, datumTerm, cause, criticalEdges);
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
        scopeTerm.getVars().stream().forEach(onFreeVar::apply);
        datumTerm.getVars().stream().forEach(onFreeVar::apply);
    }

    @Override public CNew apply(ISubstitution.Immutable subst) {
        return new CNew(
                subst.apply(scopeTerm),
                subst.apply(datumTerm),
                cause,
                ownCriticalEdges == null ? null : ownCriticalEdges.apply(subst)
        );
    }

    @Override public CNew unsafeApply(ISubstitution.Immutable subst) {
        return apply(subst);
    }

    @Override public CNew apply(IRenaming subst) {
        return new CNew(
                subst.apply(scopeTerm),
                subst.apply(datumTerm),
                cause,
                ownCriticalEdges == null ? null : ownCriticalEdges.apply(subst)
        );
    }

    @Override public String toString(TermFormatter termToString) {
        final StringBuilder sb = new StringBuilder();
        sb.append("new ");
        sb.append(termToString.format(scopeTerm));
        sb.append(" : ");
        sb.append(termToString.format(datumTerm));
        return sb.toString();
    }

    @Override public String toString() {
        return toString(ITerm::toString);
    }

    @Override public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        final CNew that = (CNew)o;
        // @formatter:off
        return this.hashCode == that.hashCode
            && Objects.equals(this.scopeTerm, that.scopeTerm)
            && Objects.equals(this.datumTerm, that.datumTerm)
            && Objects.equals(this.cause, that.cause);
        // @formatter:on
    }

    private final int hashCode = computeHashCode();

    @Override public int hashCode() {
        return hashCode;
    }

    private int computeHashCode() {
        return Objects.hash(
                scopeTerm,
                datumTerm,
                cause
        );
    }

}
