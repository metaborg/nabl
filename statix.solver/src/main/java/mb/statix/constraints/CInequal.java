package mb.statix.constraints;

import java.io.Serializable;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import com.google.common.collect.ImmutableSet;

import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.ITermVar;
import mb.nabl2.terms.substitution.IRenaming;
import mb.nabl2.terms.substitution.ISubstitution;
import mb.nabl2.util.TermFormatter;
import mb.statix.constraints.messages.IMessage;
import mb.statix.solver.IConstraint;

public class CInequal implements IConstraint, Serializable {
    private static final long serialVersionUID = 1L;

    private final Set<ITermVar> universals;
    private final ITerm term1;
    private final ITerm term2;

    private final @Nullable IConstraint cause;
    private final @Nullable IMessage message;

    public CInequal(Iterable<ITermVar> universals, ITerm term1, ITerm term2) {
        this(universals, term1, term2, null, null);
    }

    public CInequal(Iterable<ITermVar> universals, ITerm term1, ITerm term2, @Nullable IMessage message) {
        this(universals, term1, term2, null, message);
    }

    public CInequal(Iterable<ITermVar> universals, ITerm term1, ITerm term2, @Nullable IConstraint cause,
            @Nullable IMessage message) {
        this.universals = ImmutableSet.copyOf(universals);
        this.term1 = term1;
        this.term2 = term2;
        this.cause = cause;
        this.message = message;
    }

    public Set<ITermVar> universals() {
        return universals;
    }

    public ITerm term1() {
        return term1;
    }

    public ITerm term2() {
        return term2;
    }

    @Override public Optional<IConstraint> cause() {
        return Optional.ofNullable(cause);
    }

    @Override public CInequal withCause(@Nullable IConstraint cause) {
        return new CInequal(universals, term1, term2, cause, message);
    }

    @Override public Optional<IMessage> message() {
        return Optional.ofNullable(message);
    }

    @Override public CInequal withMessage(@Nullable IMessage message) {
        return new CInequal(universals, term1, term2, cause, message);
    }

    @Override public <R> R match(Cases<R> cases) {
        return cases.caseInequal(this);
    }

    @Override public <R, E extends Throwable> R matchOrThrow(CheckedCases<R, E> cases) throws E {
        return cases.caseInequal(this);
    }

    @Override public CInequal substitute(ISubstitution.Immutable subst) {
        final ISubstitution.Immutable localSubst = subst.removeAll(universals);
        final IRenaming.Immutable localRenaming = localSubst.captureAvoidingRenaming(universals);
        final Set<ITermVar> newVars =
                universals.stream().map(v -> localRenaming.apply(v)).collect(ImmutableSet.toImmutableSet());
        final ISubstitution.Immutable newSubst = localRenaming.compose(localSubst);
        return new CInequal(newVars, newSubst.apply(term1), newSubst.apply(term2), cause,
                message == null ? null : message.substitute(newSubst));
    }

    @Override public String toString(TermFormatter termToString) {
        final StringBuilder sb = new StringBuilder();
        if(!universals.isEmpty()) {
            sb.append("(forall ");
            sb.append(universals.stream().map(termToString::format).collect(Collectors.joining(", ")));
            sb.append(". ");
        }
        sb.append(termToString.format(term1));
        sb.append(" != ");
        sb.append(termToString.format(term2));
        if(!universals.isEmpty()) {
            sb.append(")");
        }
        return sb.toString();
    }

    @Override public String toString() {
        return toString(ITerm::toString);
    }

}