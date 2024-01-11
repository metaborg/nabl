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

public final class CTrue implements IConstraint, Serializable {
    private static final long serialVersionUID = 1L;

    private final @Nullable IConstraint cause;
    private final @Nullable CTrue origin;

    public CTrue() {
        this(null, null);
    }

    // Private constructor, so we can add more fields in the future. Externally call the appropriate with*() functions instead.
    private CTrue(@Nullable IConstraint cause, @Nullable CTrue origin) {
        this.cause = cause;
        this.origin = origin;
    }

    public CTrue withArguments() {
        // Avoid creating new objects.
        return this;
    }

    @Override public Optional<IConstraint> cause() {
        return Optional.ofNullable(cause);
    }

    @Override public CTrue withCause(@Nullable IConstraint cause) {
        if (this.cause == cause) {
            // Avoid creating new objects if the arguments are the exact same objects.
            // NOTE: Using `==` (instead of `Objects.equals()`) is cheap and already covers 99% of cases.
            return this;
        }
        return new CTrue(cause, origin);
    }

    @Override public @Nullable CTrue origin() {
        return origin;
    }

    @Override public <R> R match(Cases<R> cases) {
        return cases.caseTrue(this);
    }

    @Override public <R, E extends Throwable> R matchOrThrow(CheckedCases<R, E> cases) throws E {
        return cases.caseTrue(this);
    }

    @Override public Set.Immutable<ITermVar> getVars() {
        return CapsuleUtil.immutableSet();
    }

    @Override public Set.Immutable<ITermVar> freeVars() {
        Set.Transient<ITermVar> freeVars = CapsuleUtil.transientSet();
        doVisitFreeVars(freeVars::__insert);
        return freeVars.freeze();
    }

    @Override public void visitFreeVars(Action1<ITermVar> onFreeVar) {
        doVisitFreeVars(onFreeVar);
    }

    private void doVisitFreeVars(@SuppressWarnings("unused") Action1<ITermVar> onFreeVar) {
    }

    @Override public CTrue apply(ISubstitution.Immutable subst) {
        return apply(subst, false);
    }

    @Override public CTrue unsafeApply(ISubstitution.Immutable subst) {
        return unsafeApply(subst, false);
    }

    @Override public CTrue apply(IRenaming subst) {
        return apply(subst, false);
    }

    @Override public CTrue apply(@SuppressWarnings("unused") ISubstitution.Immutable subst, boolean trackOrigin) {
        return new CTrue(
                cause,
                origin == null && trackOrigin ? this : origin
        );
    }

    @Override public CTrue unsafeApply(@SuppressWarnings("unused") ISubstitution.Immutable subst, boolean trackOrigin) {
        return apply(subst, trackOrigin);
    }

    @Override public CTrue apply(@SuppressWarnings("unused") IRenaming subst, boolean trackOrigin) {
        return new CTrue(
                cause,
                origin == null && trackOrigin ? this : origin
        );
    }

    @Override public String toString(@SuppressWarnings("unused") TermFormatter termToString) {
        return "true";
    }

    @Override public String toString() {
        return toString(ITerm::toString);
    }

    @Override public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        final CTrue that = (CTrue)o;
        // @formatter:off
        return this.hashCode == that.hashCode
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
                cause,
                origin
        );
    }

}
