package mb.statix.constraints;

import java.io.Serializable;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import jakarta.annotation.Nullable;

import org.metaborg.util.collection.CapsuleUtil;
import org.metaborg.util.functions.Action1;

import io.usethesource.capsule.Set;
import io.usethesource.capsule.util.stream.CapsuleCollectors;
import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.ITermVar;
import mb.nabl2.terms.substitution.IRenaming;
import mb.nabl2.terms.substitution.ISubstitution;
import mb.nabl2.util.TermFormatter;
import mb.statix.constraints.messages.IMessage;
import mb.statix.solver.IConstraint;

public final class CInequal implements IConstraint, Serializable {
    private static final long serialVersionUID = 1L;

    private final Set<ITermVar> universals;
    private final ITerm term1;
    private final ITerm term2;

    private final @Nullable IConstraint cause;
    private final @Nullable IMessage message;
    private final @Nullable CInequal origin;

    public CInequal(Iterable<ITermVar> universals, ITerm term1, ITerm term2) {
        this(universals, term1, term2, null, null, null);
    }

    // Do not call this constructor. This is only used to reconstruct this object from a Statix term. Call withArguments() or withMessage() instead.
    public CInequal(
            Iterable<ITermVar> universals,
            ITerm term1,
            ITerm term2,
            @Nullable IMessage message
    ) {
        this(universals, term1, term2, null, message, null);
    }

    // Private constructor, so we can add more fields in the future. Externally call the appropriate with*() functions instead.
    private CInequal(
            Iterable<ITermVar> universals,
            ITerm term1,
            ITerm term2,
            @Nullable IConstraint cause,
            @Nullable IMessage message,
            @Nullable CInequal origin
    ) {
        this.universals = CapsuleUtil.toSet(universals);
        this.term1 = term1;
        this.term2 = term2;
        this.cause = cause;
        this.message = message;
        this.origin = origin;
    }

    public Set<ITermVar> universals() {
        return universals;
    }

    public ITerm term1() {
        return term1;
    }

    public ITerm term2() {
        return term2;
    }

    public CInequal withArguments(Iterable<ITermVar> universals, ITerm term1, ITerm term2) {
        if (this.universals == universals &&
            this.term1 == term1 &&
            this.term2 == term2
        ) {
            // Avoid creating new objects.
            return this;
        }
        return new CInequal(universals, term1, term2, cause, message, origin);
    }

    @Override public Optional<IConstraint> cause() {
        return Optional.ofNullable(cause);
    }

    @Override public CInequal withCause(@Nullable IConstraint cause) {
        if (this.cause == cause) {
            // Avoid creating new objects if the arguments are the exact same objects.
            // NOTE: Using `==` (instead of `Objects.equals()`) is cheap and already covers 99% of cases.
            return this;
        }
        return new CInequal(universals, term1, term2, cause, message, origin);
    }

    @Override public Optional<IMessage> message() {
        return Optional.ofNullable(message);
    }

    @Override public CInequal withMessage(@Nullable IMessage message) {
        if (this.message == message) {
            // Avoid creating new objects if the arguments are the exact same objects.
            // NOTE: Using `==` (instead of `Objects.equals()`) is cheap and already covers 99% of cases.
            return this;
        }
        return new CInequal(universals, term1, term2, cause, message, origin);
    }

    @Override public @Nullable CInequal origin() {
        return origin;
    }

    @Override public <R> R match(Cases<R> cases) {
        return cases.caseInequal(this);
    }

    @Override public <R, E extends Throwable> R matchOrThrow(CheckedCases<R, E> cases) throws E {
        return cases.caseInequal(this);
    }

    @Override public Set.Immutable<ITermVar> getVars() {
        final Set.Transient<ITermVar> vars = CapsuleUtil.transientSet();
        vars.__insertAll(universals);
        vars.__insertAll(term1.getVars());
        vars.__insertAll(term2.getVars());
        return vars.freeze();
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
        term1.getVars().stream().filter(v -> !universals.contains(v)).forEach(onFreeVar::apply);
        term2.getVars().stream().filter(v -> !universals.contains(v)).forEach(onFreeVar::apply);
        if (message != null) {
            message.visitVars(onFreeVar);
        }
    }

    @Override public CInequal apply(ISubstitution.Immutable subst) {
        return apply(subst, false);
    }

    @Override public CInequal unsafeApply(ISubstitution.Immutable subst) {
        return unsafeApply(subst, false);
    }

    @Override public CInequal apply(IRenaming subst) {
        return apply(subst, false);
    }

    @Override public CInequal apply(ISubstitution.Immutable subst, boolean trackOrigin) {
        final Set.Immutable<ITermVar> us = universals.stream()
                .flatMap(v -> subst.apply(v).getVars().stream())
                .collect(CapsuleCollectors.toSet());
        return new CInequal(
                us,
                subst.apply(term1),
                subst.apply(term2),
                cause,
                message == null ? null : message.apply(subst),
                origin == null && trackOrigin ? this : origin
        );
    }

    @Override public CInequal unsafeApply(ISubstitution.Immutable subst, boolean trackOrigin) {
        return apply(subst, trackOrigin);
    }

    @Override public CInequal apply(IRenaming subst, boolean trackOrigin) {
        final Set.Immutable<ITermVar> us = universals.stream()
                .map(subst::rename)
                .collect(CapsuleCollectors.toSet());
        return new CInequal(
                us,
                subst.apply(term1),
                subst.apply(term2),
                cause,
                message == null ? null : message.apply(subst),
                origin == null && trackOrigin ? this : origin
        );
    }

    @Override public String toString(TermFormatter termToString) {
        final StringBuilder sb = new StringBuilder();
        if (!universals.isEmpty()) {
            sb.append("(forall ");
            sb.append(universals.stream().map(termToString::format).collect(Collectors.joining(", ")));
            sb.append(". ");
        }
        sb.append(termToString.format(term1));
        sb.append(" != ");
        sb.append(termToString.format(term2));
        if (!universals.isEmpty()) {
            sb.append(")");
        }
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
        final CInequal that = (CInequal)o;
        // @formatter:off
        return this.hashCode == that.hashCode
            && Objects.equals(this.universals, that.universals)
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
                universals,
                term1,
                term2,
                cause,
                message,
                origin
        );
    }

}
