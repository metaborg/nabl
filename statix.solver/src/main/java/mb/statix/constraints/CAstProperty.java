package mb.statix.constraints;

import java.io.Serializable;
import java.util.Objects;
import java.util.Optional;

import jakarta.annotation.Nullable;

import org.metaborg.util.collection.CapsuleUtil;
import org.metaborg.util.functions.Action1;

import io.usethesource.capsule.Set;
import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.ITermVar;
import mb.nabl2.terms.substitution.IRenaming;
import mb.nabl2.terms.substitution.ISubstitution;
import mb.nabl2.util.TermFormatter;
import mb.statix.solver.IConstraint;

public final class CAstProperty implements IConstraint, Serializable {
    private static final long serialVersionUID = 1L;

    public enum Op {
        SET {
            @Override public String toString() {
                return ":=";
            }
        },
        ADD {
            @Override public String toString() {
                return "+=";
            }
        }
    }

    private final ITerm idTerm;
    private final ITerm property;
    private final Op op;
    private final ITerm value;

    private final @Nullable IConstraint cause;

    public CAstProperty(ITerm idTerm, ITerm property, Op op, ITerm value) {
        this(idTerm, property, op, value, null);
    }

    // Private constructor, so we can add more fields in the future. Externally call the appropriate with*() functions instead.
    private CAstProperty(
            ITerm idTerm,
            ITerm property,
            Op op,
            ITerm value,
            @Nullable IConstraint cause
    ) {
        this.idTerm = idTerm;
        this.property = property;
        this.op = op;
        this.value = value;
        this.cause = cause;
    }

    public ITerm idTerm() {
        return idTerm;
    }

    public ITerm property() {
        return property;
    }

    public Op op() {
        return op;
    }

    public ITerm value() {
        return value;
    }

    public CAstProperty withArguments(ITerm idTerm, ITerm property, Op op, ITerm value) {
        return new CAstProperty(idTerm, property, op, value, cause);
    }

    @Override public Optional<IConstraint> cause() {
        return Optional.ofNullable(cause);
    }

    @Override public CAstProperty withCause(@Nullable IConstraint cause) {
        return new CAstProperty(idTerm, property, op, value, cause);
    }

    @Override public <R> R match(Cases<R> cases) {
        return cases.caseTermProperty(this);
    }

    @Override public <R, E extends Throwable> R matchOrThrow(CheckedCases<R, E> cases) throws E {
        return cases.caseTermProperty(this);
    }

    @Override public Set.Immutable<ITermVar> getVars() {
        return Set.Immutable.union(
                idTerm.getVars(),
                value.getVars()
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
        idTerm.getVars().forEach(onFreeVar::apply);
        value.getVars().forEach(onFreeVar::apply);
    }

    @Override public CAstProperty apply(ISubstitution.Immutable subst) {
        return new CAstProperty(subst.apply(idTerm), property, op, subst.apply(value), cause);
    }

    @Override public CAstProperty unsafeApply(ISubstitution.Immutable subst) {
        return apply(subst);
    }

    @Override public CAstProperty apply(IRenaming subst) {
        return new CAstProperty(subst.apply(idTerm), property, op, subst.apply(value), cause);
    }

    @Override public String toString(TermFormatter termToString) {
        final StringBuilder sb = new StringBuilder();
        sb.append("@");
        sb.append(termToString.format(idTerm));
        sb.append(".");
        sb.append(property.toString());
        sb.append(" ").append(op).append(" ");
        sb.append(termToString.format(value));
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
        final CAstProperty that = (CAstProperty)o;
        // @formatter:off
        return this.hashCode == that.hashCode
            && Objects.equals(this.idTerm, that.idTerm)
            && Objects.equals(this.property, that.property)
            && this.op == that.op
            && Objects.equals(this.value, that.value)
            && Objects.equals(this.cause, that.cause);
        // @formatter:on
    }

    private final int hashCode = computeHashCode();

    @Override public int hashCode() {
        return hashCode;
    }

    private int computeHashCode() {
        return Objects.hash(
                idTerm,
                property,
                op,
                value,
                cause
        );
    }

}
