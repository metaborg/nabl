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
import mb.nabl2.util.CapsuleUtil;
import mb.nabl2.util.TermFormatter;
import mb.statix.solver.IConstraint;

public class CExists implements IConstraint, Serializable {
    private static final long serialVersionUID = 1L;

    private final Set.Immutable<ITermVar> vars;
    private final IConstraint constraint;

    private final @Nullable IConstraint cause;

    public CExists(Iterable<ITermVar> vars, IConstraint constraint) {
        this(vars, constraint, null);
    }

    public CExists(Iterable<ITermVar> vars, IConstraint constraint, @Nullable IConstraint cause) {
        this.vars = CapsuleUtil.toSet(vars);
        this.constraint = constraint;
        this.cause = cause;
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
        return new CExists(vars, constraint, cause);
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
        return new CExists(vars, constraint.apply(subst.removeAll(vars)), cause);
    }

    @Override public CExists apply(IRenaming subst) {
        return new CExists(vars, constraint.apply(subst), cause);
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

    @Override public int hashCode() {
        return Objects.hash(vars, constraint, cause);
    }
}
