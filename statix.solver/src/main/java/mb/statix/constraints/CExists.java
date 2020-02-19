package mb.statix.constraints;

import java.io.Serializable;
import java.util.Optional;
import java.util.Set;

import javax.annotation.Nullable;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;

import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.ITermVar;
import mb.nabl2.terms.substitution.IRenaming;
import mb.nabl2.terms.substitution.ISubstitution;
import mb.nabl2.util.TermFormatter;
import mb.statix.solver.IConstraint;

public class CExists implements IConstraint, Serializable {
    private static final long serialVersionUID = 1L;

    private final Set<ITermVar> vars;
    private final IConstraint constraint;

    private final @Nullable IConstraint cause;

    public CExists(Iterable<ITermVar> vars, IConstraint constraint) {
        this(vars, constraint, null);
    }

    public CExists(Iterable<ITermVar> vars, IConstraint constraint, @Nullable IConstraint cause) {
        this.vars = ImmutableSet.copyOf(vars);
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

    @Override public Set<ITermVar> boundVars() {
        return vars;
    }

    @Override public Set<ITermVar> freeVars() {
        final ImmutableSet.Builder<ITermVar> freeVars = ImmutableSet.builder();
        freeVars.addAll(constraint.freeVars());
        return Sets.difference(freeVars.build(), boundVars()).immutableCopy();
    }

    @Override public CExists doSubstitute(IRenaming.Immutable localRenaming, ISubstitution.Immutable totalSubst) {
        final Set<ITermVar> newVars =
                vars.stream().map(v -> localRenaming.apply(v)).collect(ImmutableSet.toImmutableSet());
        return new CExists(newVars, constraint.recSubstitute(totalSubst), cause);
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

}