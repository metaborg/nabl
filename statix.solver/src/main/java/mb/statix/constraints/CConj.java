package mb.statix.constraints;

import java.io.Serializable;
import java.util.Optional;
import java.util.Set;

import javax.annotation.Nullable;

import com.google.common.collect.ImmutableSet;

import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.ITermVar;
import mb.nabl2.terms.substitution.IRenaming;
import mb.nabl2.terms.substitution.ISubstitution;
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

    @Override public Set<ITermVar> boundVars() {
        return ImmutableSet.of();
    }

    @Override public Set<ITermVar> freeVars() {
        final ImmutableSet.Builder<ITermVar> freeVars = ImmutableSet.builder();
        freeVars.addAll(left.freeVars());
        freeVars.addAll(right.freeVars());
        return freeVars.build();
    }

    @Override public CConj doSubstitute(IRenaming.Immutable localRenaming, ISubstitution.Immutable totalSubst) {
        return new CConj(left.recSubstitute(totalSubst), right.recSubstitute(totalSubst), cause);
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

}