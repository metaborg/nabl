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

public final class CAstId implements IConstraint, Serializable {
    private static final long serialVersionUID = 1L;

    private final ITerm term;
    private final ITerm idTerm;

    private final @Nullable IConstraint cause;
    private final @Nullable CAstId origin;

    public CAstId(ITerm term, ITerm idTerm) {
        this(term, idTerm, null, null);
    }

    // Private constructor, so we can add more fields in the future. Externally call the appropriate with*() functions instead.
    private CAstId(ITerm term, ITerm idTerm, @Nullable IConstraint cause, @Nullable CAstId origin) {
        this.term = term;
        this.idTerm = idTerm;
        this.cause = cause;
        this.origin = origin;
    }

    public ITerm astTerm() {
        return term;
    }

    public ITerm idTerm() {
        return idTerm;
    }

    public CAstId withArguments(ITerm term, ITerm idTerm) {
        if (this.term == term &&
            this.idTerm == idTerm
        ) {
            // Avoid creating new objects if the arguments are the exact same objects.
            // NOTE: Using `==` (instead of `Objects.equals()`) is cheap and already covers 99% of cases.
            return this;
        }
        return new CAstId(term, idTerm, cause, origin);
    }

    @Override public Optional<IConstraint> cause() {
        return Optional.ofNullable(cause);
    }

    @Override public CAstId withCause(@Nullable IConstraint cause) {
        if (this.cause == cause) {
            // Avoid creating new objects if the arguments are the exact same objects.
            // NOTE: Using `==` (instead of `Objects.equals()`) is cheap and already covers 99% of cases.
            return this;
        }
        return new CAstId(term, idTerm, cause, origin);
    }

    @Override public @Nullable CAstId origin() {
        return origin;
    }

    @Override public <R> R match(Cases<R> cases) {
        return cases.caseTermId(this);
    }

    @Override public <R, E extends Throwable> R matchOrThrow(CheckedCases<R, E> cases) throws E {
        return cases.caseTermId(this);
    }

    @Override public Set.Immutable<ITermVar> getVars() {
        return Set.Immutable.union(
                term.getVars(),
                idTerm.getVars()
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
        term.getVars().forEach(onFreeVar::apply);
        idTerm.getVars().forEach(onFreeVar::apply);
    }

    @Override public CAstId apply(ISubstitution.Immutable subst) {
        return apply(subst, false);
    }

    @Override public CAstId unsafeApply(ISubstitution.Immutable subst) {
        return unsafeApply(subst, false);
    }

    @Override public CAstId apply(IRenaming subst) {
        return apply(subst, false);
    }

    @Override public CAstId apply(ISubstitution.Immutable subst, boolean trackOrigin) {
        return new CAstId(
                subst.apply(term),
                subst.apply(idTerm),
                cause,
                origin == null && trackOrigin ? this : origin
        );
    }

    @Override public CAstId unsafeApply(ISubstitution.Immutable subst, boolean trackOrigin) {
        return apply(subst, trackOrigin);
    }

    @Override public CAstId apply(IRenaming subst, boolean trackOrigin) {
        return new CAstId(
                subst.apply(term),
                subst.apply(idTerm),
                cause,
                origin == null && trackOrigin ? this : origin
        );
    }

    @Override public String toString(TermFormatter termToString) {
        final StringBuilder sb = new StringBuilder();
        sb.append("termId(");
        sb.append(termToString.format(term));
        sb.append(", ");
        sb.append(termToString.format(idTerm));
        sb.append(")");
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
        final CAstId that = (CAstId)o;
        // @formatter:off
        return this.hashCode == that.hashCode
            && Objects.equals(this.term, that.term)
            && Objects.equals(this.idTerm, that.idTerm)
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
                term,
                idTerm,
                cause,
                origin
        );
    }

}
