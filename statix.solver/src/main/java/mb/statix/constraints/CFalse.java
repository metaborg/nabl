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

public final class CFalse implements IConstraint, Serializable {
    private static final long serialVersionUID = 1L;

    private final @Nullable IConstraint cause;
    private final @Nullable IMessage message;
    private final @Nullable CFalse origin;

    public CFalse() {
        this(null, null, null);
    }

    // This constructor is primarily used to reconstruct this object from a Statix term. Call withMessage() instead.
    public CFalse(@Nullable IMessage message) {
        this(null, message, null);
    }

    // Private constructor, so we can add more fields in the future. Externally call the appropriate with*() functions instead.
    private CFalse(
            @Nullable IConstraint cause,
            @Nullable IMessage message,
            @Nullable CFalse origin
    ) {
        this.cause = cause;
        this.message = message;
        this.origin = origin;
    }

    public CFalse withArguments() {
        // Avoid creating new objects.
        return this;
    }

    @Override public Optional<IConstraint> cause() {
        return Optional.ofNullable(cause);
    }

    @Override public CFalse withCause(@Nullable IConstraint cause) {
        if (this.cause == cause) {
            // Avoid creating new objects if the arguments are the exact same objects.
            // NOTE: Using `==` (instead of `Objects.equals()`) is cheap and already covers 99% of cases.
            return this;
        }
        return new CFalse(cause, message, origin);
    }

    @Override public Optional<IMessage> message() {
        return Optional.ofNullable(message);
    }

    @Override public CFalse withMessage(@Nullable IMessage message) {
        if (this.message == message) {
            // Avoid creating new objects if the arguments are the exact same objects.
            // NOTE: Using `==` (instead of `Objects.equals()`) is cheap and already covers 99% of cases.
            return this;
        }
        return new CFalse(cause, message, origin);
    }

    @Override public @Nullable CFalse origin() {
        return origin;
    }

    @Override public <R> R match(Cases<R> cases) {
        return cases.caseFalse(this);
    }

    @Override public <R, E extends Throwable> R matchOrThrow(CheckedCases<R, E> cases) throws E {
        return cases.caseFalse(this);
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

    private void doVisitFreeVars(Action1<ITermVar> onFreeVar) {
        if (message != null) {
            message.visitVars(onFreeVar);
        }
    }

    @Override public CFalse apply(ISubstitution.Immutable subst) {
        return apply(subst, false);
    }

    @Override public CFalse unsafeApply(ISubstitution.Immutable subst) {
        return unsafeApply(subst, false);
    }

    @Override public CFalse apply(IRenaming subst) {
        return apply(subst, false);
    }

    @Override public CFalse apply(ISubstitution.Immutable subst, boolean trackOrigin) {
        return new CFalse(
                cause,
                message == null ? null : message.apply(subst),
                origin == null && trackOrigin ? this : origin
        );
    }

    @Override public CFalse unsafeApply(ISubstitution.Immutable subst, boolean trackOrigin) {
        return apply(subst, trackOrigin);
    }

    @Override public CFalse apply(IRenaming subst, boolean trackOrigin) {
        return new CFalse(
                cause,
                message == null ? null : message.apply(subst),
                origin == null && trackOrigin ? this : origin
        );
    }

    @Override public String toString(@SuppressWarnings("unused") TermFormatter termToString) {
        return "false";
    }

    @Override public String toString() {
        return toString(ITerm::toString);
    }

    @Override public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        final CFalse that = (CFalse)o;
        // @formatter:off
        return this.hashCode == that.hashCode
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
                cause,
                message,
                origin
        );
    }

}
