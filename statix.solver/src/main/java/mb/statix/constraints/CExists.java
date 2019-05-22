package mb.statix.constraints;

import java.io.Serializable;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javax.annotation.Nullable;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.ITermVar;
import mb.nabl2.terms.substitution.ISubstitution;
import mb.nabl2.terms.substitution.PersistentSubstitution;
import mb.nabl2.util.TermFormatter;
import mb.statix.solver.Delay;
import mb.statix.solver.IConstraint;
import mb.statix.taico.solver.IMState;
import mb.statix.taico.solver.MConstraintContext;
import mb.statix.taico.solver.MConstraintResult;

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
    
    @Override
    public Optional<MConstraintResult> solve(IMState state, MConstraintContext params)
            throws InterruptedException, Delay {
        final ImmutableMap.Builder<ITermVar, ITermVar> existentialsBuilder = ImmutableMap.builder();
        for(ITermVar var : vars()) {
            final ITermVar freshVar = state.freshVar(var.getName());
            existentialsBuilder.put(var, freshVar);
        }
        final Map<ITermVar, ITermVar> existentials = existentialsBuilder.build();
        final ISubstitution.Immutable subst = PersistentSubstitution.Immutable.of(existentials);
        final IConstraint newConstraint = constraint().apply(subst).withCause(this);
        return Optional.of(MConstraintResult.ofConstraints(newConstraint).withExistentials(existentials));
    }

    @Override public CExists apply(ISubstitution.Immutable subst) {
        return new CExists(vars, constraint.apply(subst.removeAll(vars)), cause);
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