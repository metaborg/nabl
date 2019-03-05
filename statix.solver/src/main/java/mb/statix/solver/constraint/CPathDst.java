package mb.statix.solver.constraint;

import java.util.Optional;

import javax.annotation.Nullable;

import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.substitution.ISubstitution;
import mb.nabl2.util.TermFormatter;
import mb.statix.solver.IConstraint;

public class CPathDst implements IConstraint {

    private final ITerm pathTerm;
    private final ITerm dstTerm;

    private final @Nullable IConstraint cause;

    public CPathDst(ITerm pathTerm, ITerm dstTerm) {
        this(pathTerm, dstTerm, null);
    }

    public CPathDst(ITerm pathTerm, ITerm dstTerm, @Nullable IConstraint cause) {
        this.pathTerm = pathTerm;
        this.dstTerm = dstTerm;
        this.cause = cause;
    }

    public ITerm pathTerm() {
        return pathTerm;
    }

    public ITerm dstTerm() {
        return dstTerm;
    }

    @Override public Optional<IConstraint> cause() {
        return Optional.ofNullable(cause);
    }

    @Override public CPathDst withCause(@Nullable IConstraint cause) {
        return new CPathDst(pathTerm, dstTerm, cause);
    }

    @Override public <R> R match(Cases<R> cases) {
        return cases.casePathDst(this);
    }

    @Override public <R, E extends Throwable> R matchOrThrow(CheckedCases<R, E> cases) throws E {
        return cases.casePathDst(this);
    }

    @Override public CPathDst apply(ISubstitution.Immutable subst) {
        return new CPathDst(subst.apply(pathTerm), subst.apply(dstTerm), cause);
    }

    @Override public String toString(TermFormatter termToString) {
        final StringBuilder sb = new StringBuilder();
        sb.append("dst(");
        sb.append(termToString.format(pathTerm));
        sb.append(", ");
        sb.append(termToString.format(dstTerm));
        sb.append(")");
        return sb.toString();
    }

    @Override public String toString() {
        return toString(ITerm::toString);
    }

}