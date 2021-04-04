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
import mb.statix.arithmetic.ArithExpr;
import mb.statix.arithmetic.ArithTest;
import mb.statix.constraints.messages.IMessage;
import mb.statix.solver.IConstraint;

public class CArith implements IConstraint, Serializable {
    private static final long serialVersionUID = 1L;

    private final ArithExpr expr1;
    private final ArithTest op;
    private final ArithExpr expr2;

    private final @Nullable IConstraint cause;
    private final @Nullable IMessage message;

    public CArith(ArithExpr expr1, ArithTest op, ArithExpr expr2) {
        this(expr1, op, expr2, null, null);
    }

    public CArith(ArithExpr expr1, ArithTest op, ArithExpr expr2, @Nullable IMessage message) {
        this(expr1, op, expr2, null, message);
    }

    private CArith(ArithExpr expr1, ArithTest op, ArithExpr expr2, @Nullable IConstraint cause,
            @Nullable IMessage message) {
        this.expr1 = expr1;
        this.op = op;
        this.expr2 = expr2;
        this.cause = cause;
        this.message = message;
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

    @Override public Optional<IConstraint> cause() {
        return Optional.ofNullable(cause);
    }

    @Override public CArith withCause(@Nullable IConstraint cause) {
        return new CArith(expr1, op, expr2, cause, message);
    }

    @Override public Optional<IMessage> message() {
        return Optional.ofNullable(message);
    }

    @Override public CArith withMessage(@Nullable IMessage message) {
        return new CArith(expr1, op, expr2, cause, message);
    }

    @Override public <R> R match(Cases<R> cases) {
        return cases.caseArith(this);
    }

    @Override public <R, E extends Throwable> R matchOrThrow(CheckedCases<R, E> cases) throws E {
        return cases.caseArith(this);
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
        if(message != null) {
            message.visitVars(onFreeVar);
        }
    }

    @Override public CArith apply(ISubstitution.Immutable subst) {
        return new CArith(expr1.apply(subst), op, expr2.apply(subst), cause,
                message == null ? null : message.apply(subst));
    }

    @Override public CArith apply(IRenaming subst) {
        return new CArith(expr1.apply(subst), op, expr2.apply(subst), cause,
                message == null ? null : message.apply(subst));
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
        if(this == o)
            return true;
        if(o == null || getClass() != o.getClass())
            return false;
        CArith cArith = (CArith) o;
        return Objects.equals(expr1, cArith.expr1) && Objects.equals(op, cArith.op)
                && Objects.equals(expr2, cArith.expr2) && Objects.equals(cause, cArith.cause)
                && Objects.equals(message, cArith.message);
    }

    private volatile int hashCode;

    @Override public int hashCode() {
        int result = hashCode;
        if(result == 0) {
            result = Objects.hash(expr1, op, expr2, cause, message);
            hashCode = result;
        }
        return result;
    }

}
