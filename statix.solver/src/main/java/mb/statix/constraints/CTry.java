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

    @Override public Set<ITermVar> boundVars() {
        return ImmutableSet.of();
    }

    @Override public Set<ITermVar> freeVars() {
        final ImmutableSet.Builder<ITermVar> freeVars = ImmutableSet.builder();
        freeVars.addAll(constraint.freeVars());
        message().ifPresent(m -> freeVars.addAll(m.freeVars()));
        return freeVars.build();
    }

    @Override public CTry doSubstitute(IRenaming.Immutable localRenaming, ISubstitution.Immutable totalSubst) {
        return new CTry(constraint.recSubstitute(totalSubst), cause,
                message == null ? null : message.recSubstitute(totalSubst));
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

}