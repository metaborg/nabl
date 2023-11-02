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
import mb.statix.arithmetic.ArithExpr;
import mb.statix.arithmetic.ArithTest;
import mb.statix.constraints.messages.IMessage;
import mb.statix.solver.IConstraint;

public final class CArith implements IConstraint, Serializable {
    private static final long serialVersionUID = 1L;

    private final ArithExpr expr1;
    private final ArithTest op;
    private final ArithExpr expr2;

    private final @Nullable IConstraint cause;
    private final @Nullable IMessage message;
    private final @Nullable CArith origin;

    public CArith(ArithExpr expr1, ArithTest op, ArithExpr expr2) {
        this(expr1, op, expr2, null, null, null);
    }

    // Do not call this constructor. This is only used to reconstruct this object from a Statix term. Call withArguments() or withMessage() instead.
    public CArith(ArithExpr expr1, ArithTest op, ArithExpr expr2, @Nullable IMessage message) {
        this(expr1, op, expr2, null, message, null);
    }

    // Private constructor, so we can add more fields in the future. Externally call the appropriate with*() functions instead.
    private CArith(
            ArithExpr expr1,
            ArithTest op,
            ArithExpr expr2,
            @Nullable IConstraint cause,
            @Nullable IMessage message,
            @Nullable CArith origin
    ) {
        this.expr1 = expr1;
        this.op = op;
        this.expr2 = expr2;
        this.cause = cause;
        this.message = message;
        this.origin = origin;
    }

    public ArithExpr expr1() {
        return expr1;
    }

    public ArithTest op() {
        return op;
    }

    public ArithExpr expr2() {
        return expr2;
    }

    public CArith withArguments(ArithExpr expr1, ArithTest op, ArithExpr expr2) {
        if (this.expr1 == expr1 &&
            this.op == op &&
            this.expr2 == expr2
        ) {
            // Avoid creating new objects if the arguments are the exact same objects.
            // NOTE: Using `==` (instead of `Objects.equals()`) is cheap and already covers 99% of cases.
            return this;
        }
        return new CArith(expr1, op, expr2, cause, message, origin);
    }

    @Override public Optional<IConstraint> cause() {
        return Optional.ofNullable(cause);
    }

    @Override public CArith withCause(@Nullable IConstraint cause) {
        if (this.cause == cause) {
            // Avoid creating new objects if the arguments are the exact same objects.
            // NOTE: Using `==` (instead of `Objects.equals()`) is cheap and already covers 99% of cases.
            return this;
        }
        return new CArith(expr1, op, expr2, cause, message, origin);
    }

    @Override public Optional<IMessage> message() {
        return Optional.ofNullable(message);
    }

    @Override public CArith withMessage(@Nullable IMessage message) {
        if (this.message == message) {
            // Avoid creating new objects if the arguments are the exact same objects.
            // NOTE: Using `==` (instead of `Objects.equals()`) is cheap and already covers 99% of cases.
            return this;
        }
        return new CArith(expr1, op, expr2, cause, message, origin);
    }

    @Override public @Nullable CArith origin() {
        return origin;
    }

    @Override public <R> R match(Cases<R> cases) {
        return cases.caseArith(this);
    }

    @Override public <R, E extends Throwable> R matchOrThrow(CheckedCases<R, E> cases) throws E {
        return cases.caseArith(this);
    }

    @Override public Set.Immutable<ITermVar> getVars() {
        return Set.Immutable.union(
                expr1.getVars(),
                expr2.getVars()
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
        expr1.isTerm().ifPresent(t -> t.getVars().forEach(onFreeVar::apply));
        expr2.isTerm().ifPresent(t -> t.getVars().forEach(onFreeVar::apply));
        if (message != null) {
            message.visitVars(onFreeVar);
        }
    }

    @Override public CArith apply(ISubstitution.Immutable subst) {
        return apply(subst, false);
    }

    @Override public CArith unsafeApply(ISubstitution.Immutable subst) {
        return unsafeApply(subst, false);
    }

    @Override public CArith apply(IRenaming subst) {
        return apply(subst, false);
    }

    @Override public CArith apply(ISubstitution.Immutable subst, boolean trackOrigin) {
        return new CArith(
                expr1.apply(subst),
                op,
                expr2.apply(subst),
                cause,
                message == null ? null : message.apply(subst),
                origin == null && trackOrigin ? this : origin
        );
    }

    @Override public CArith unsafeApply(ISubstitution.Immutable subst, boolean trackOrigin) {
        return apply(subst, trackOrigin);
    }

    @Override public CArith apply(IRenaming subst, boolean trackOrigin) {
        return new CArith(
                expr1.apply(subst),
                op,
                expr2.apply(subst),
                cause,
                message == null ? null : message.apply(subst),
                origin == null && trackOrigin ? this : origin
        );
    }

    @Override public String toString(TermFormatter termToString) {
        final StringBuilder sb = new StringBuilder();
        sb.append(expr1.toString(termToString));
        sb.append(" #").append(op).append(" ");
        sb.append(expr2.toString(termToString));
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
        final CArith that = (CArith)o;
        // @formatter:off
        return this.hashCode == that.hashCode
            && Objects.equals(this.expr1, that.expr1)
            && Objects.equals(this.op, that.op)
            && Objects.equals(this.expr2, that.expr2)
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
        return  Objects.hash(
                expr1,
                op,
                expr2,
                cause,
                message,
                origin
        );
    }

}
