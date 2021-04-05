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
import mb.statix.solver.IConstraint;

public class CConj implements IConstraint, Serializable {
    private static final long serialVersionUID = 1L;

    private final IConstraint left;
    private final IConstraint right;

    private final @Nullable IConstraint cause;

    public CConj(IConstraint left, IConstraint right) {
        this(left, right, null);
    }

    public CConj(IConstraint left, IConstraint right, @Nullable IConstraint cause) {
        this.left = left;
        this.right = right;
        this.cause = cause;
    }

    public IConstraint left() {
        return left;
    }

    public IConstraint right() {
        return right;
    }

    @Override public Optional<IConstraint> cause() {
        return Optional.ofNullable(cause);
    }

    @Override public CConj withCause(@Nullable IConstraint cause) {
        return new CConj(left, right, cause);
    }

    @Override public <R> R match(Cases<R> cases) {
        return cases.caseConj(this);
    }

    @Override public <R, E extends Throwable> R matchOrThrow(CheckedCases<R, E> cases) throws E {
        return cases.caseConj(this);
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
        left.visitFreeVars(onFreeVar);
        right.visitFreeVars(onFreeVar);
    }

    @Override public CConj apply(ISubstitution.Immutable subst) {
        return new CConj(left.apply(subst), right.apply(subst), cause);
    }

    @Override public CConj unsafeApply(ISubstitution.Immutable subst) {
        return new CConj(left.unsafeApply(subst), right.unsafeApply(subst), cause);
    }

    @Override public CConj apply(IRenaming subst) {
        return new CConj(left.apply(subst), right.apply(subst), cause);
    }

    @Override public String toString(TermFormatter termToString) {
        final StringBuilder sb = new StringBuilder();
        sb.append(left.toString(termToString));
        sb.append(", ");
        sb.append(right.toString(termToString));
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
        CConj cConj = (CConj) o;
        return Objects.equals(left, cConj.left) && Objects.equals(right, cConj.right)
                && Objects.equals(cause, cConj.cause);
    }

    private volatile int hashCode;

    @Override public int hashCode() {
        int result = hashCode;
        if(result == 0) {
            result = Objects.hash(left, right, cause);
            hashCode = result;
        }
        return result;
    }

}
