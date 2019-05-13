package mb.statix.solver.constraint;

import java.io.Serializable;
import java.util.Optional;

import javax.annotation.Nullable;

import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.substitution.ISubstitution;
import mb.nabl2.util.TermFormatter;
import mb.statix.solver.IConstraint;

public class CTermProperty implements IConstraint, Serializable {
    private static final long serialVersionUID = 1L;

    private final ITerm idTerm;
    private final ITerm property;
    private final ITerm value;

    private final @Nullable IConstraint cause;

    public CTermProperty(ITerm idTerm, ITerm property, ITerm value) {
        this(idTerm, property, value, null);
    }

    public CTermProperty(ITerm idTerm, ITerm property, ITerm value, @Nullable IConstraint cause) {
        this.idTerm = idTerm;
        this.property = property;
        this.value = value;
        this.cause = cause;
    }

    public ITerm idTerm() {
        return idTerm;
    }

    public ITerm property() {
        return property;
    }

    public ITerm value() {
        return value;
    }

    @Override public Optional<IConstraint> cause() {
        return Optional.ofNullable(cause);
    }

    @Override public CTermProperty withCause(@Nullable IConstraint cause) {
        return new CTermProperty(idTerm, property, value, cause);
    }

    @Override public <R> R match(Cases<R> cases) {
        return cases.caseTermProperty(this);
    }

    @Override public <R, E extends Throwable> R matchOrThrow(CheckedCases<R, E> cases) throws E {
        return cases.caseTermProperty(this);
    }

    @Override public CTermProperty apply(ISubstitution.Immutable subst) {
        return new CTermProperty(subst.apply(idTerm), property, subst.apply(value), cause);
    }

    @Override public String toString(TermFormatter termToString) {
        final StringBuilder sb = new StringBuilder();
        sb.append("@");
        sb.append(termToString.format(idTerm));
        sb.append(".");
        sb.append(property.toString());
        sb.append(" := ");
        sb.append(termToString.format(value));
        return sb.toString();
    }

    @Override public String toString() {
        return toString(ITerm::toString);
    }

}