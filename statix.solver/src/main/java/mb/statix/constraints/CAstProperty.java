package mb.statix.constraints;

import java.io.Serializable;
import java.util.Optional;

import javax.annotation.Nullable;

import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.substitution.ISubstitution;
import mb.nabl2.util.TermFormatter;
import mb.statix.solver.IConstraint;

public class CAstProperty implements IConstraint, Serializable {
    private static final long serialVersionUID = 1L;

    private final ITerm idTerm;
    private final ITerm property;
    private final ITerm value;

    private final @Nullable IConstraint cause;

    public CAstProperty(ITerm idTerm, ITerm property, ITerm value) {
        this(idTerm, property, value, null);
    }

    public CAstProperty(ITerm idTerm, ITerm property, ITerm value, @Nullable IConstraint cause) {
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

    @Override public CAstProperty withCause(@Nullable IConstraint cause) {
        return new CAstProperty(idTerm, property, value, cause);
    }

    @Override public <R> R match(Cases<R> cases) {
        return cases.caseTermProperty(this);
    }

    @Override public <R, E extends Throwable> R matchOrThrow(CheckedCases<R, E> cases) throws E {
        return cases.caseTermProperty(this);
    }

    @Override public CAstProperty substitute(ISubstitution.Immutable subst) {
        return new CAstProperty(subst.apply(idTerm), property, subst.apply(value), cause);
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