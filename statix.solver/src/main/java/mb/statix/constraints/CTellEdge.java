package mb.statix.constraints;

import java.io.Serializable;
import java.util.Objects;
import java.util.Optional;

import jakarta.annotation.Nullable;

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

public final class CTellEdge implements IConstraint, Serializable {
    private static final long serialVersionUID = 1L;

    private final ITerm sourceTerm;
    private final ITerm label;
    private final ITerm targetTerm;

    private final @Nullable IConstraint cause;
    private final @Nullable CTellEdge origin;
    private final @Nullable ICompleteness.Immutable ownCriticalEdges;

    public CTellEdge(ITerm sourceTerm, ITerm label, ITerm targetTerm) {
        this(sourceTerm, label, targetTerm, null, null, null);
    }

    // Private constructor, so we can add more fields in the future. Externally call the appropriate with*() functions instead.
    private CTellEdge(
            ITerm sourceTerm,
            ITerm label,
            ITerm targetTerm,
            @Nullable IConstraint cause,
            @Nullable CTellEdge origin,
            @Nullable ICompleteness.Immutable ownCriticalEdges
    ) {
        this.sourceTerm = sourceTerm;
        this.label = label;
        this.targetTerm = targetTerm;
        this.cause = cause;
        this.origin = origin;
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

    public CTellEdge withArguments(ITerm sourceTerm, ITerm label, ITerm targetTerm) {
        if (this.sourceTerm == sourceTerm &&
            this.label == label &&
            this.targetTerm == targetTerm
        ) {
            // Avoid creating new objects if the arguments are the exact same objects.
            // NOTE: Using `==` (instead of `Objects.equals()`) is cheap and already covers 99% of cases.
            return this;
        }
        return new CTellEdge(sourceTerm, label, targetTerm, cause, origin, ownCriticalEdges);
    }

    @Override public Optional<IConstraint> cause() {
        return Optional.ofNullable(cause);
    }

    @Override public CTellEdge withCause(@Nullable IConstraint cause) {
        if (this.cause == cause) {
            // Avoid creating new objects if the arguments are the exact same objects.
            // NOTE: Using `==` (instead of `Objects.equals()`) is cheap and already covers 99% of cases.
            return this;
        }
        return new CTellEdge(sourceTerm, label, targetTerm, cause, origin, ownCriticalEdges);
    }

    @Override public @Nullable CTellEdge origin() {
        return origin;
    }

    @Override public Optional<ICompleteness.Immutable> ownCriticalEdges() {
        return Optional.ofNullable(ownCriticalEdges);
    }

    @Override public CTellEdge withOwnCriticalEdges(ICompleteness.Immutable criticalEdges) {
        if (this.ownCriticalEdges == criticalEdges) {
            // Avoid creating new objects if the arguments are the exact same objects.
            // NOTE: Using `==` (instead of `Objects.equals()`) is cheap and already covers 99% of cases.
            return this;
        }
        return new CTellEdge(sourceTerm, label, targetTerm, cause, origin, criticalEdges);
    }

    @Override public <R> R match(Cases<R> cases) {
        return cases.caseTellEdge(this);
    }

    @Override public <R, E extends Throwable> R matchOrThrow(CheckedCases<R, E> cases) throws E {
        return cases.caseTellEdge(this);
    }

    @Override public Set.Immutable<ITermVar> getVars() {
        return Set.Immutable.union(
                sourceTerm.getVars(),
                targetTerm.getVars()
        );
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
        return apply(subst, false);
    }

    @Override public CTellEdge unsafeApply(ISubstitution.Immutable subst) {
        return unsafeApply(subst, false);
    }

    @Override public CTellEdge apply(IRenaming subst) {
        return apply(subst, false);
    }

    @Override public CTellEdge apply(ISubstitution.Immutable subst, boolean trackOrigin) {
        return new CTellEdge(
                subst.apply(sourceTerm),
                label,
                subst.apply(targetTerm),
                cause,
                origin == null && trackOrigin ? this : origin,
                ownCriticalEdges == null ? null : ownCriticalEdges.apply(subst)
        );
    }

    @Override public CTellEdge unsafeApply(ISubstitution.Immutable subst, boolean trackOrigin) {
        return apply(subst, trackOrigin);
    }

    @Override public CTellEdge apply(IRenaming subst, boolean trackOrigin) {
        return new CTellEdge(
                subst.apply(sourceTerm),
                label,
                subst.apply(targetTerm),
                cause,
                origin == null && trackOrigin ? this : origin,
                ownCriticalEdges == null ? null : ownCriticalEdges.apply(subst)
        );
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
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        CTellEdge that = (CTellEdge)o;
        // @formatter:off
        return this.hashCode == that.hashCode
            && Objects.equals(this.sourceTerm, that.sourceTerm)
            && Objects.equals(this.label, that.label)
            && Objects.equals(this.targetTerm, that.targetTerm)
            && Objects.equals(this.cause, that.cause)
            && Objects.equals(this.origin, that.origin);
        // @formatter:on
    }

    private final int hashCode = computeHashCode();

    @Override public int hashCode() {
        return hashCode;
    }

    private int computeHashCode() {
        return Objects.hash(
                sourceTerm,
                label,
                targetTerm,
                cause,
                origin
        );
    }

}
