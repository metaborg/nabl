package mb.statix.solver.constraint;

import java.io.Serializable;
import java.util.Optional;

import javax.annotation.Nullable;

import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.substitution.ISubstitution;
import mb.nabl2.util.TermFormatter;
import mb.statix.solver.IConstraint;

public class CPathSrc implements IConstraint, Serializable {
    private static final long serialVersionUID = 1L;

    private final ITerm pathTerm;
    private final ITerm srcTerm;

    private final @Nullable IConstraint cause;

    public CPathSrc(ITerm pathTerm, ITerm srcTerm) {
        this(pathTerm, srcTerm, null);
    }

    public CPathSrc(ITerm pathTerm, ITerm srcTerm, @Nullable IConstraint cause) {
        this.pathTerm = pathTerm;
        this.srcTerm = srcTerm;
        this.cause = cause;
    }

    public ITerm pathTerm() {
        return pathTerm;
    }

    public ITerm srcTerm() {
        return srcTerm;
    }

    @Override public Optional<IConstraint> cause() {
        return Optional.ofNullable(cause);
    }

    @Override public CPathSrc withCause(@Nullable IConstraint cause) {
        return new CPathSrc(pathTerm, srcTerm, cause);
    }

    @Override public <R> R match(Cases<R> cases) {
        return cases.casePathSrc(this);
    }

    @Override public <R, E extends Throwable> R matchOrThrow(CheckedCases<R, E> cases) throws E {
        return cases.casePathSrc(this);
    }

    @Override public CPathSrc apply(ISubstitution.Immutable subst) {
        return new CPathSrc(subst.apply(pathTerm), subst.apply(srcTerm), cause);
    }

    @Override public String toString(TermFormatter termToString) {
        final StringBuilder sb = new StringBuilder();
        sb.append("src(");
        sb.append(termToString.format(pathTerm));
        sb.append(", ");
        sb.append(termToString.format(srcTerm));
        sb.append(")");
        return sb.toString();
    }

    @Override public String toString() {
        return toString(ITerm::toString);
    }

}