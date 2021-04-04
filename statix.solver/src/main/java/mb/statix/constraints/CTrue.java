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

public class CTrue implements IConstraint, Serializable {
    private static final long serialVersionUID = 1L;

    private final @Nullable IConstraint cause;

    public CTrue() {
        this(null);
    }

    public CTrue(@Nullable IConstraint cause) {
        this.cause = cause;
    }

    @Override public Optional<IConstraint> cause() {
        return Optional.ofNullable(cause);
    }

    @Override public CTrue withCause(@Nullable IConstraint cause) {
        return new CTrue(cause);
    }

    @Override public <R> R match(Cases<R> cases) {
        return cases.caseTrue(this);
    }

    @Override public <R, E extends Throwable> R matchOrThrow(CheckedCases<R, E> cases) throws E {
        return cases.caseTrue(this);
    }

    @Override public Set.Immutable<ITermVar> freeVars() {
        Set.Transient<ITermVar> freeVars = CapsuleUtil.transientSet();
        doVisitFreeVars(freeVars::__insert);
        return freeVars.freeze();
    }

    @Override public void visitFreeVars(Action1<ITermVar> onFreeVar) {
        doVisitFreeVars(onFreeVar);
    }

    private void doVisitFreeVars(@SuppressWarnings("unused") Action1<ITermVar> onFreeVar) {
    }

    @Override public CTrue apply(@SuppressWarnings("unused") ISubstitution.Immutable subst) {
        return this;
    }

    @Override public CTrue apply(@SuppressWarnings("unused") IRenaming subst) {
        return this;
    }

    @Override public String toString(@SuppressWarnings("unused") TermFormatter termToString) {
        return "true";
    }

    @Override public String toString() {
        return toString(ITerm::toString);
    }

    @Override public boolean equals(Object o) {
        if(this == o)
            return true;
        if(o == null || getClass() != o.getClass())
            return false;
        CTrue cTrue = (CTrue) o;
        return Objects.equals(cause, cTrue.cause);
    }

    private volatile int hashCode;

    @Override public int hashCode() {
        int result = hashCode;
        if(result == 0) {
            result = Objects.hash(cause);
            hashCode = result;
        }
        return result;
    }

}
