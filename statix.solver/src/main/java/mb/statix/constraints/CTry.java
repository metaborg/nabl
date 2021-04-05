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
import mb.statix.constraints.messages.IMessage;
import mb.statix.solver.IConstraint;

public class CTry implements IConstraint, Serializable {
    private static final long serialVersionUID = 1L;

    private final IConstraint constraint;

    private final @Nullable IConstraint cause;
    private final @Nullable IMessage message;

    public CTry(IConstraint constraint) {
        this(constraint, null, null);
    }

    public CTry(IConstraint constraint, @Nullable IMessage message) {
        this(constraint, null, message);
    }

    public CTry(IConstraint constraint, @Nullable IConstraint cause, @Nullable IMessage message) {
        this.constraint = constraint;
        this.cause = cause;
        this.message = message;
    }

    public IConstraint constraint() {
        return constraint;
    }

    @Override public Optional<IConstraint> cause() {
        return Optional.ofNullable(cause);
    }

    @Override public CTry withCause(@Nullable IConstraint cause) {
        return new CTry(constraint, cause, message);
    }

    @Override public Optional<IMessage> message() {
        return Optional.ofNullable(message);
    }

    @Override public CTry withMessage(@Nullable IMessage message) {
        return new CTry(constraint, cause, message);
    }

    @Override public <R> R match(Cases<R> cases) {
        return cases.caseTry(this);
    }

    @Override public <R, E extends Throwable> R matchOrThrow(CheckedCases<R, E> cases) throws E {
        return cases.caseTry(this);
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
        if(message != null) {
            message.visitVars(onFreeVar);
        }
    }

    @Override public CTry apply(ISubstitution.Immutable subst) {
        return new CTry(constraint.apply(subst), cause, message == null ? null : message.apply(subst));
    }

    @Override public CTry unsafeApply(ISubstitution.Immutable subst) {
        return new CTry(constraint.unsafeApply(subst), cause, message == null ? null : message.apply(subst));
    }

    @Override public CTry apply(IRenaming subst) {
        return new CTry(constraint.apply(subst), cause, message == null ? null : message.apply(subst));
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
        if(this == o)
            return true;
        if(o == null || getClass() != o.getClass())
            return false;
        CTry cTry = (CTry) o;
        return Objects.equals(constraint, cTry.constraint) && Objects.equals(cause, cTry.cause)
                && Objects.equals(message, cTry.message);
    }

    private volatile int hashCode;

    @Override public int hashCode() {
        int result = hashCode;
        if(result == 0) {
            result = Objects.hash(constraint, cause, message);
            hashCode = result;
        }
        return result;
    }

}
