package mb.statix.constraints;

import java.io.Serializable;
import java.util.Optional;

import javax.annotation.Nullable;

import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.substitution.ISubstitution;
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

    @Override public CArith apply(ISubstitution.Immutable subst) {
        return new CArith(expr1.apply(subst), op, expr2.apply(subst), cause, message == null ? null : message.apply(subst));
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

}