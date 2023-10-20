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
import mb.statix.solver.IConstraint;

public final class CConj implements IConstraint, Serializable {
    private static final long serialVersionUID = 1L;

    private final IConstraint left;
    private final IConstraint right;

    private final @Nullable IConstraint cause;

    public CConj(IConstraint left, IConstraint right) {
        this(left, right, null);
    }

    // Do not call this constructor. Call withArguments() or withCause() instead.
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

    public CConj withArguments(IConstraint left, IConstraint right) {
        return new CConj(left, right, cause);
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

    @Override public Set.Immutable<ITermVar> getVars() {
        return Set.Immutable.union(
                left.getVars(),
                right.getVars()
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
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        final CConj that = (CConj)o;
        // @formatter:off
        return this.hashCode == that.hashCode
            && Objects.equals(this.left, that.left)
            && Objects.equals(this.right, that.right)
            && Objects.equals(this.cause, that.cause);
        // @formatter:on
    }

    private final int hashCode = computeHashCode();

    @Override public int hashCode() {
        return hashCode;
    }

    private int computeHashCode() {
        return Objects.hash(
                left,
                right,
                cause
        );
    }

}
