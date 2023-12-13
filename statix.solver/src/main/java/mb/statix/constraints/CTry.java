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
import mb.statix.constraints.messages.IMessage;
import mb.statix.solver.IConstraint;

public final class CTry implements IConstraint, Serializable {
    private static final long serialVersionUID = 1L;

    private final IConstraint constraint;

    private final @Nullable IConstraint cause;
    private final @Nullable IMessage message;
    private final @Nullable CTry origin;

    public CTry(IConstraint constraint) {
        this(constraint, null, null, null);
    }

    // Do not call this constructor. This is only used to reconstruct this object from a Statix term. Call withArguments() or withMessage() instead.
    public CTry(IConstraint constraint, @Nullable IMessage message) {
        this(constraint, null, message, null);
    }

    // Private constructor, so we can add more fields in the future. Externally call the appropriate with*() functions instead.
    private CTry(IConstraint constraint, @Nullable IConstraint cause, @Nullable IMessage message, @Nullable CTry origin) {
        this.constraint = constraint;
        this.cause = cause;
        this.message = message;
        this.origin = origin;
    }

    public IConstraint constraint() {
        return constraint;
    }

    public CTry withArguments(IConstraint constraint) {
        if (this.constraint == constraint) {
            // Avoid creating new objects if the arguments are the exact same objects.
            // NOTE: Using `==` (instead of `Objects.equals()`) is cheap and already covers 99% of cases.
            return this;
        }
        return new CTry(constraint, cause, message, origin);
    }

    @Override public Optional<IConstraint> cause() {
        return Optional.ofNullable(cause);
    }

    @Override public CTry withCause(@Nullable IConstraint cause) {
        if (this.cause == cause) {
            // Avoid creating new objects if the arguments are the exact same objects.
            // NOTE: Using `==` (instead of `Objects.equals()`) is cheap and already covers 99% of cases.
            return this;
        }
        return new CTry(constraint, cause, message, origin);
    }

    @Override public Optional<IMessage> message() {
        return Optional.ofNullable(message);
    }

    @Override public CTry withMessage(@Nullable IMessage message) {
        if (this.message == message) {
            // Avoid creating new objects if the arguments are the exact same objects.
            // NOTE: Using `==` (instead of `Objects.equals()`) is cheap and already covers 99% of cases.
            return this;
        }
        return new CTry(constraint, cause, message, origin);
    }

    @Override public @Nullable CTry origin() {
        return origin;
    }

    @Override public <R> R match(Cases<R> cases) {
        return cases.caseTry(this);
    }

    @Override public <R, E extends Throwable> R matchOrThrow(CheckedCases<R, E> cases) throws E {
        return cases.caseTry(this);
    }

    @Override public Set.Immutable<ITermVar> getVars() {
        return constraint.getVars();
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
        constraint.visitFreeVars(onFreeVar);
        if (message != null) {
            message.visitVars(onFreeVar);
        }
    }

    @Override public CTry apply(ISubstitution.Immutable subst) {
        return apply(subst, false);
    }

    @Override public CTry unsafeApply(ISubstitution.Immutable subst) {
        return unsafeApply(subst, false);
    }

    @Override public CTry apply(IRenaming subst) {
        return apply(subst, false);
    }

    @Override public CTry apply(ISubstitution.Immutable subst, boolean trackOrigin) {
        return new CTry(
                constraint.apply(subst, trackOrigin),
                cause,
                message == null ? null : message.apply(subst),
                origin == null && trackOrigin ? this : origin
        );
    }

    @Override public CTry unsafeApply(ISubstitution.Immutable subst, boolean trackOrigin) {
        return new CTry(
                constraint.unsafeApply(subst, trackOrigin),
                cause,
                message == null ? null : message.apply(subst),
                origin == null && trackOrigin ? this : origin
        );
    }

    @Override public CTry apply(IRenaming subst, boolean trackOrigin) {
        return new CTry(
                constraint.apply(subst, trackOrigin),
                cause,
                message == null ? null : message.apply(subst),
                origin == null && trackOrigin ? this : origin
        );
    }

    @Override public String toString(TermFormatter termToString) {
        final StringBuilder sb = new StringBuilder();
        sb.append("try (");
        sb.append(constraint.toString(termToString));
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
        final CTry that = (CTry)o;
        // @formatter:off
        return this.hashCode == that.hashCode
            && Objects.equals(this.constraint, that.constraint)
            && Objects.equals(this.cause, that.cause)
            && Objects.equals(this.message, that.message)
            && Objects.equals(this.origin, that.origin);
        // @formatter:on
    }

    private final int hashCode = computeHashCode();

    @Override public int hashCode() {
        return hashCode;
    }

    private int computeHashCode() {
        return Objects.hash(
                constraint,
                cause,
                message,
                origin
        );
    }

}
