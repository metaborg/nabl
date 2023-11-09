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

public final class CEqual implements IConstraint, Serializable {
    private static final long serialVersionUID = 1L;

    private final ITerm term1;
    private final ITerm term2;

    private final @Nullable IConstraint cause;
    private final @Nullable IMessage message;
    private final @Nullable CEqual origin;

    public CEqual(ITerm term1, ITerm term2) {
        this(term1, term2, null, null, null);
    }

    // Do not call this constructor. This is only used to reconstruct this object from a Statix term. Call withArguments() or withMessage() instead.
    public CEqual(ITerm term1, ITerm term2, @Nullable IMessage message) {
        this(term1, term2, null, message, null);
    }

    // Do not call this constructor. This is used in the solver. Call withArguments() or withCause() instead.
    public CEqual(ITerm term1, ITerm term2, @Nullable IConstraint cause) {
        this(term1, term2, cause, null, null);
    }

    // Private constructor, so we can add more fields in the future. Externally call the appropriate with*() functions instead.
    private CEqual(
            ITerm term1,
            ITerm term2,
            @Nullable IConstraint cause,
            @Nullable IMessage message,
            @Nullable CEqual origin
    ) {
        this.term1 = term1;
        this.term2 = term2;
        this.cause = cause;
        this.message = message;
        this.origin = origin;
    }

    public ITerm term1() {
        return term1;
    }

    public ITerm term2() {
        return term2;
    }

    public CEqual withArguments(ITerm term1, ITerm term2) {
        if (this.term1 == term1 &&
            this.term2 == term2
        ) {
            // Avoid creating new objects if the arguments are the exact same objects.
            // NOTE: Using `==` (instead of `Objects.equals()`) is cheap and already covers 99% of cases.
            return this;
        }
        return new CEqual(term1, term2, cause, message, origin);
    }

    @Override public Optional<IConstraint> cause() {
        return Optional.ofNullable(cause);
    }

    @Override public CEqual withCause(@Nullable IConstraint cause) {
        if (this.cause == cause) {
            // Avoid creating new objects if the arguments are the exact same objects.
            // NOTE: Using `==` (instead of `Objects.equals()`) is cheap and already covers 99% of cases.
            return this;
        }
        return new CEqual(term1, term2, cause, message, origin);
    }

    @Override public Optional<IMessage> message() {
        return Optional.ofNullable(message);
    }

    @Override public CEqual withMessage(@Nullable IMessage message) {
        if (this.message == message) {
            // Avoid creating new objects if the arguments are the exact same objects.
            // NOTE: Using `==` (instead of `Objects.equals()`) is cheap and already covers 99% of cases.
            return this;
        }
        return new CEqual(term1, term2, cause, message, origin);
    }

    @Override public @Nullable CEqual origin() {
        return origin;
    }

    @Override public <R> R match(Cases<R> cases) {
        return cases.caseEqual(this);
    }

    @Override public <R, E extends Throwable> R matchOrThrow(CheckedCases<R, E> cases) throws E {
        return cases.caseEqual(this);
    }

    @Override public Set.Immutable<ITermVar> getVars() {
        return Set.Immutable.union(
                term1.getVars(),
                term2.getVars()
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
        term1.getVars().forEach(onFreeVar::apply);
        term2.getVars().forEach(onFreeVar::apply);
        if (message != null) {
            message.visitVars(onFreeVar);
        }
    }

    @Override public CEqual apply(ISubstitution.Immutable subst) {
        return apply(subst, false);
    }

    @Override public CEqual unsafeApply(ISubstitution.Immutable subst) {
        return unsafeApply(subst, false);
    }

    @Override public CEqual apply(IRenaming subst) {
        return apply(subst, false);
    }

    @Override public CEqual apply(ISubstitution.Immutable subst, boolean trackOrigin) {
        return new CEqual(
                subst.apply(term1),
                subst.apply(term2),
                cause,
                message == null ? null : message.apply(subst),
                origin == null && trackOrigin ? this : origin
        );
    }

    @Override public CEqual unsafeApply(ISubstitution.Immutable subst, boolean trackOrigin) {
        return apply(subst, trackOrigin);
    }

    @Override public CEqual apply(IRenaming subst, boolean trackOrigin) {
        return new CEqual(
                subst.apply(term1),
                subst.apply(term2),
                cause,
                message == null ? null : message.apply(subst),
                origin == null && trackOrigin ? this : origin
        );
    }

    @Override public String toString(TermFormatter termToString) {
        final StringBuilder sb = new StringBuilder();
        sb.append(termToString.format(term1));
        sb.append(" == ");
        sb.append(termToString.format(term2));
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
        final CEqual that = (CEqual)o;
        // @formatter:off
        return this.hashCode == that.hashCode
            && Objects.equals(this.term1, that.term1)
            && Objects.equals(this.term2, that.term2)
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
                term1,
                term2,
                cause,
                message,
                origin
        );
    }

}
