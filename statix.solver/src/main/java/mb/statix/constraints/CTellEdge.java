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
import mb.statix.solver.IConstraint;
import mb.statix.solver.completeness.ICompleteness;

public class CTellEdge implements IConstraint, Serializable {
    private static final long serialVersionUID = 1L;

    private final ITerm sourceTerm;
    private final ITerm label;
    private final ITerm targetTerm;

    private final @Nullable IConstraint cause;
    private final @Nullable ICompleteness.Immutable ownCriticalEdges;

    public CTellEdge(ITerm sourceTerm, ITerm label, ITerm targetTerm) {
        this(sourceTerm, label, targetTerm, null, null);
    }

    public CTellEdge(ITerm sourceTerm, ITerm label, ITerm targetTerm, @Nullable IConstraint cause,
            @Nullable ICompleteness.Immutable ownCriticalEdges) {
        this.sourceTerm = sourceTerm;
        this.label = label;
        this.targetTerm = targetTerm;
        this.cause = cause;
        this.ownCriticalEdges = ownCriticalEdges;
    }

    public ITerm sourceTerm() {
        return sourceTerm;
    }

    public ITerm label() {
        return label;
    }

    public ITerm targetTerm() {
        return targetTerm;
    }

    @Override public Optional<IConstraint> cause() {
        return Optional.ofNullable(cause);
    }

    @Override public CTellEdge withCause(@Nullable IConstraint cause) {
        return new CTellEdge(sourceTerm, label, targetTerm, cause, ownCriticalEdges);
    }

    @Override public Optional<ICompleteness.Immutable> ownCriticalEdges() {
        return Optional.ofNullable(ownCriticalEdges);
    }

    @Override public CTellEdge withOwnCriticalEdges(ICompleteness.Immutable criticalEdges) {
        return new CTellEdge(sourceTerm, label, targetTerm, cause, criticalEdges);
    }

    @Override public <R> R match(Cases<R> cases) {
        return cases.caseTellEdge(this);
    }

    @Override public <R, E extends Throwable> R matchOrThrow(CheckedCases<R, E> cases) throws E {
        return cases.caseTellEdge(this);
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
        sourceTerm.getVars().forEach(onFreeVar::apply);
        targetTerm.getVars().forEach(onFreeVar::apply);
    }

    @Override public CTellEdge apply(ISubstitution.Immutable subst) {
        return new CTellEdge(subst.apply(sourceTerm), label, subst.apply(targetTerm), cause,
                ownCriticalEdges == null ? null : ownCriticalEdges.apply(subst));
    }

    @Override public CTellEdge apply(IRenaming subst) {
        return new CTellEdge(subst.apply(sourceTerm), label, subst.apply(targetTerm), cause,
                ownCriticalEdges.apply(subst));
    }

    @Override public String toString(TermFormatter termToString) {
        final StringBuilder sb = new StringBuilder();
        sb.append(termToString.format(sourceTerm));
        sb.append(" -");
        sb.append(termToString.format(label));
        sb.append("-> ");
        sb.append(termToString.format(targetTerm));
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
        CTellEdge cTellEdge = (CTellEdge) o;
        return Objects.equals(sourceTerm, cTellEdge.sourceTerm) && Objects.equals(label, cTellEdge.label)
                && Objects.equals(targetTerm, cTellEdge.targetTerm) && Objects.equals(cause, cTellEdge.cause);
    }

    private volatile int hashCode;

    @Override public int hashCode() {
        int result = hashCode;
        if(result == 0) {
            result = Objects.hash(sourceTerm, label, targetTerm, cause);
            hashCode = result;
        }
        return result;
    }

}
