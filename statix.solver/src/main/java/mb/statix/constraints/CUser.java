package mb.statix.constraints;

import java.io.Serializable;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import jakarta.annotation.Nullable;

import org.metaborg.util.collection.CapsuleUtil;
import org.metaborg.util.collection.ImList;
import org.metaborg.util.functions.Action1;

import io.usethesource.capsule.Set;
import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.ITermVar;
import mb.nabl2.terms.substitution.IRenaming;
import mb.nabl2.terms.substitution.ISubstitution;
import mb.nabl2.util.TermFormatter;
import mb.statix.constraints.messages.IMessage;
import mb.statix.solver.IConstraint;
import mb.statix.solver.completeness.ICompleteness;

public final class CUser implements IConstraint, Serializable {
    private static final long serialVersionUID = 1L;

    private final String name;
    private final List<ITerm> args;

    private final @Nullable IConstraint cause;
    private final @Nullable IMessage message;
    private final @Nullable CUser origin;
    private final @Nullable ICompleteness.Immutable ownCriticalEdges;

    public CUser(String name, Iterable<? extends ITerm> args) {
        this(name, args, null, null, null, null);
    }

    // Do not call this constructor. This is only used to reconstruct this object from a Statix term. Call withArguments() or withMessage() instead.
    public CUser(String name, Iterable<? extends ITerm> args, @Nullable IMessage message) {
        this(name, args, null, message, null, null);
    }

    // Private constructor, so we can add more fields in the future. Externally call the appropriate with*() functions instead.
    private CUser(
            String name,
            Iterable<? extends ITerm> args,
            @Nullable IConstraint cause,
            @Nullable IMessage message,
            @Nullable CUser origin,
            @Nullable ICompleteness.Immutable ownCriticalEdges
    ) {
        this.name = name;
        this.args = ImList.Immutable.copyOf(args);
        this.cause = cause;
        this.message = message;
        this.origin = origin;
        this.ownCriticalEdges = ownCriticalEdges;
    }

    public String name() {
        return name;
    }

    public List<ITerm> args() {
        return args;
    }

    public CUser withArguments(String name, Iterable<? extends ITerm> args) {
        //noinspection StringEquality
        if (this.name == name &&
            this.args == args
        ) {
            // Avoid creating new objects if the arguments are the exact same objects.
            // NOTE: Using `==` (instead of `Objects.equals()`) is cheap and already covers 99% of cases.
            return this;
        }
        return new CUser(name, args, cause, message, origin, ownCriticalEdges);
    }

    @Override public Optional<IConstraint> cause() {
        return Optional.ofNullable(cause);
    }

    @Override public CUser withCause(@Nullable IConstraint cause) {
        if (this.cause == cause) {
            // Avoid creating new objects if the arguments are the exact same objects.
            // NOTE: Using `==` (instead of `Objects.equals()`) is cheap and already covers 99% of cases.
            return this;
        }
        return new CUser(name, args, cause, message, origin, ownCriticalEdges);
    }

    @Override public Optional<IMessage> message() {
        return Optional.ofNullable(message);
    }

    @Override public CUser withMessage(@Nullable IMessage message) {
        if (this.message == message) {
            // Avoid creating new objects if the arguments are the exact same objects.
            // NOTE: Using `==` (instead of `Objects.equals()`) is cheap and already covers 99% of cases.
            return this;
        }
        return new CUser(name, args, cause, message, origin, ownCriticalEdges);
    }

    @Override public @Nullable CUser origin() {
        return origin;
    }

    @Override public Optional<ICompleteness.Immutable> ownCriticalEdges() {
        return Optional.ofNullable(ownCriticalEdges);
    }

    @Override public CUser withOwnCriticalEdges(ICompleteness.Immutable criticalEdges) {
        if (this.ownCriticalEdges == criticalEdges) {
            // Avoid creating new objects if the arguments are the exact same objects.
            // NOTE: Using `==` (instead of `Objects.equals()`) is cheap and already covers 99% of cases.
            return this;
        }
        return new CUser(name, args, cause, message, origin, criticalEdges);
    }

    @Override public <R> R match(Cases<R> cases) {
        return cases.caseUser(this);
    }

    @Override public <R, E extends Throwable> R matchOrThrow(CheckedCases<R, E> cases) throws E {
        return cases.caseUser(this);
    }

    @Override public Set.Immutable<ITermVar> getVars() {
        final Set.Transient<ITermVar> vars = CapsuleUtil.transientSet();
        for (ITerm a : args) {
            vars.__insertAll(a.getVars());
        }
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
        args.forEach(t -> t.getVars().forEach(onFreeVar::apply));
        if (message != null) {
            message.visitVars(onFreeVar);
        }
    }

    @Override public CUser apply(ISubstitution.Immutable subst) {
        return apply(subst, false);
    }

    @Override public CUser unsafeApply(ISubstitution.Immutable subst) {
        return unsafeApply(subst, false);
    }

    @Override public CUser apply(IRenaming subst) {
        return apply(subst, false);
    }

    @Override public CUser apply(ISubstitution.Immutable subst, boolean trackOrigin) {
        return new CUser(
                name,
                subst.apply(args),
                cause,
                message == null ? null : message.apply(subst),
                origin == null && trackOrigin ? this : origin,
                ownCriticalEdges == null ? null : ownCriticalEdges.apply(subst)
        );
    }

    @Override public CUser unsafeApply(ISubstitution.Immutable subst, boolean trackOrigin) {
        return apply(subst, trackOrigin);
    }

    @Override public CUser apply(IRenaming subst, boolean trackOrigin) {
        return new CUser(
                name,
                subst.apply(args),
                cause,
                message == null ? null : message.apply(subst),
                origin == null && trackOrigin ? this : origin,
                ownCriticalEdges == null ? null : ownCriticalEdges.apply(subst)
        );
    }

    @Override public String toString(TermFormatter termToString) {
        final StringBuilder sb = new StringBuilder();
        sb.append(name);
        sb.append("(");
        sb.append(termToString.format(args));
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
        final CUser that = (CUser)o;
        // @formatter:off
        return this.hashCode == that.hashCode
            && Objects.equals(this.name, that.name)
            && Objects.equals(this.args, that.args)
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
                name,
                args,
                cause,
                message,
                origin
        );
    }

}
