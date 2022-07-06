package mb.statix.constraints;

import java.io.Serializable;
import java.util.Objects;
import java.util.Optional;

import javax.annotation.Nullable;

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

public class CFalse implements IConstraint, Serializable {
    private static final long serialVersionUID = 1L;

    private final @Nullable IConstraint cause;
    private final @Nullable IMessage message;

    public CFalse() {
        this(null, null);
    }

    public CFalse(@Nullable IMessage message) {
        this(null, message);
    }

    public CFalse(@Nullable IConstraint cause, @Nullable IMessage message) {
        this.cause = cause;
        this.message = message;
    }

    @Override public Optional<IConstraint> cause() {
        return Optional.ofNullable(cause);
    }

    @Override public CFalse withCause(@Nullable IConstraint cause) {
        return new CFalse(cause, message);
    }

    @Override public Optional<IMessage> message() {
        return Optional.ofNullable(message);
    }

    @Override public CFalse withMessage(@Nullable IMessage message) {
        return new CFalse(cause, message);
    }

    @Override public <R> R match(Cases<R> cases) {
        return cases.caseFalse(this);
    }

    @Override public <R, E extends Throwable> R matchOrThrow(CheckedCases<R, E> cases) throws E {
        return cases.caseFalse(this);
    }

    @Override public Set.Immutable<ITermVar> getVars() {
        return Set.Immutable.of();
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
        if(message != null) {
            message.visitVars(onFreeVar);
        }
    }

    @Override public CFalse apply(ISubstitution.Immutable subst) {
        return new CFalse(cause, message == null ? null : message.apply(subst));
    }

    @Override public CFalse unsafeApply(ISubstitution.Immutable subst) {
        return new CFalse(cause, message == null ? null : message.apply(subst));
    }

    @Override public CFalse apply(IRenaming subst) {
        return new CFalse(cause, message == null ? null : message.apply(subst));
    }

    @Override public String toString(@SuppressWarnings("unused") TermFormatter termToString) {
        return "false";
    }

    @Override public String toString() {
        return toString(ITerm::toString);
    }

    @Override public boolean equals(Object o) {
        if(this == o)
            return true;
        if(o == null || getClass() != o.getClass())
            return false;
        CFalse cFalse = (CFalse) o;
        return Objects.equals(cause, cFalse.cause) && Objects.equals(message, cFalse.message);
    }

    private volatile int hashCode;

    @Override public int hashCode() {
        int result = hashCode;
        if(result == 0) {
            result = Objects.hash(cause, message);
            hashCode = result;
        }
        return result;
    }

}
