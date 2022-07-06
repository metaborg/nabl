package mb.statix.constraints;

import java.io.Serializable;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import org.metaborg.util.collection.CapsuleUtil;
import org.metaborg.util.functions.Action1;

import io.usethesource.capsule.Set;
import io.usethesource.capsule.util.stream.CapsuleCollectors;
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
        this.universals = CapsuleUtil.toSet(universals);
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

    @Override public Set.Immutable<ITermVar> getVars() {
        final Set.Transient<ITermVar> vars = Set.Transient.of();
        vars.__insertAll(universals);
        vars.__insertAll(term1.getVars());
        vars.__insertAll(term2.getVars());
        return vars.freeze();
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
        term1.getVars().stream().filter(v -> !universals.contains(v)).forEach(onFreeVar::apply);
        term2.getVars().stream().filter(v -> !universals.contains(v)).forEach(onFreeVar::apply);
        if(message != null) {
            message.visitVars(onFreeVar);
        }

    }

    @Override public CInequal apply(ISubstitution.Immutable subst) {
        final Set.Immutable<ITermVar> us =
                universals.stream().flatMap(v -> subst.apply(v).getVars().stream()).collect(CapsuleCollectors.toSet());
        return new CInequal(us, subst.apply(term1), subst.apply(term2), cause,
                message == null ? null : message.apply(subst));
    }

    @Override public CInequal unsafeApply(ISubstitution.Immutable subst) {
        final Set.Immutable<ITermVar> us =
                universals.stream().flatMap(v -> subst.apply(v).getVars().stream()).collect(CapsuleCollectors.toSet());
        return new CInequal(us, subst.apply(term1), subst.apply(term2), cause,
                message == null ? null : message.apply(subst));
    }

    @Override public CInequal apply(IRenaming subst) {
        final Set.Immutable<ITermVar> us =
                universals.stream().map(v -> subst.rename(v)).collect(CapsuleCollectors.toSet());
        return new CInequal(us, subst.apply(term1), subst.apply(term2), cause,
                message == null ? null : message.apply(subst));
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

    @Override public boolean equals(Object o) {
        if(this == o)
            return true;
        if(o == null || getClass() != o.getClass())
            return false;
        CInequal cInequal = (CInequal) o;
        return Objects.equals(universals, cInequal.universals) && Objects.equals(term1, cInequal.term1)
                && Objects.equals(term2, cInequal.term2) && Objects.equals(cause, cInequal.cause)
                && Objects.equals(message, cInequal.message);
    }

    private volatile int hashCode;

    @Override public int hashCode() {
        int result = hashCode;
        if(result == 0) {
            result = Objects.hash(universals, term1, term2, cause, message);
            hashCode = result;
        }
        return result;
    }

}
