package mb.statix.constraints;

import java.io.Serializable;
import java.util.Objects;
import java.util.Optional;

import javax.annotation.Nullable;

import io.usethesource.capsule.Set;
import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.ITermVar;
import mb.nabl2.terms.substitution.IRenaming;
import mb.nabl2.terms.substitution.ISubstitution;
import mb.nabl2.terms.substitution.ISubstitution.Immutable;
import mb.nabl2.util.CapsuleUtil;
import mb.nabl2.util.TermFormatter;
import mb.statix.solver.IConstraint;
import mb.statix.solver.completeness.ICompleteness;

public class CExists implements IConstraint, Serializable {
    private static final long serialVersionUID = 1L;

    private final Set.Immutable<ITermVar> vars;
    private final IConstraint constraint;

    private final @Nullable IConstraint cause;
    private final @Nullable ICompleteness.Immutable bodyCriticalEdges;

    public CExists(Iterable<ITermVar> vars, IConstraint constraint) {
        this(vars, constraint, null, null);
    }

    public CExists(Iterable<ITermVar> vars, IConstraint constraint, @Nullable IConstraint cause,
            @Nullable ICompleteness.Immutable bodyCriticalEdges) {
        this.vars = CapsuleUtil.toSet(vars);
        this.constraint = constraint;
        this.cause = cause;
        this.bodyCriticalEdges = bodyCriticalEdges;
    }

    public Set<ITermVar> vars() {
        return vars;
    }

    public IConstraint constraint() {
        return constraint;
    }

    @Override public Optional<IConstraint> cause() {
        return Optional.ofNullable(cause);
    }

    @Override public CExists withCause(@Nullable IConstraint cause) {
        return new CExists(vars, constraint, cause, bodyCriticalEdges);
    }

    @Override public Optional<ICompleteness.Immutable> bodyCriticalEdges() {
        return Optional.ofNullable(bodyCriticalEdges);
    }

    @Override public CExists withBodyCriticalEdges(ICompleteness.Immutable criticalEdges) {
        return new CExists(vars, constraint, cause, criticalEdges);
    }

    @Override public <R> R match(Cases<R> cases) {
        return cases.caseExists(this);
    }

    @Override public <R, E extends Throwable> R matchOrThrow(CheckedCases<R, E> cases) throws E {
        return cases.caseExists(this);
    }

    @Override public Set.Immutable<ITermVar> getVars() {
        return Set.Immutable.union(vars, constraint.getVars());
    }

    @Override public CExists apply(ISubstitution.Immutable subst) {
        final Immutable localSubst = subst.removeAll(vars);
        return new CExists(vars, constraint.apply(localSubst), cause,
                bodyCriticalEdges == null ? null : bodyCriticalEdges.apply(localSubst));
    }

    @Override public CExists apply(IRenaming subst) {
        return new CExists(vars, constraint.apply(subst), cause,
                bodyCriticalEdges == null ? null : bodyCriticalEdges.apply(subst));
    }

    @Override public String toString(TermFormatter termToString) {
        final StringBuilder sb = new StringBuilder();
        sb.append("{").append(termToString.format(vars)).append("} ");
        sb.append(constraint.toString(termToString));
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
        CExists cExists = (CExists) o;
        return Objects.equals(vars, cExists.vars) && Objects.equals(constraint, cExists.constraint)
                && Objects.equals(cause, cExists.cause);
    }

    private volatile int hashCode;

    @Override public int hashCode() {
        int result = hashCode;
        if(result == 0) {
            result = Objects.hash(vars, constraint, cause);
            hashCode = result;
        }
        return result;
    }

}
