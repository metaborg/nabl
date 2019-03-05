package mb.statix.solver.constraint;

import java.util.Optional;

import javax.annotation.Nullable;

import mb.nabl2.relations.IRelation;
import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.substitution.ISubstitution;
import mb.nabl2.util.TermFormatter;
import mb.statix.solver.IConstraint;

public class CPathLt implements IConstraint {

    private final IRelation.Immutable<ITerm> lt;
    private final ITerm label1Term;
    private final ITerm label2Term;

    private final @Nullable IConstraint cause;

    public CPathLt(IRelation.Immutable<ITerm> lt, ITerm l1, ITerm l2) {
        this(lt, l1, l2, null);
    }

    public CPathLt(IRelation.Immutable<ITerm> lt, ITerm l1, ITerm l2, @Nullable IConstraint cause) {
        this.lt = lt;
        this.label1Term = l1;
        this.label2Term = l2;
        this.cause = cause;
    }

    public IRelation.Immutable<ITerm> lt() {
        return lt;
    }

    public ITerm label1Term() {
        return label1Term;
    }

    public ITerm label2Term() {
        return label2Term;
    }

    @Override public Optional<IConstraint> cause() {
        return Optional.ofNullable(cause);
    }

    @Override public CPathLt withCause(@Nullable IConstraint cause) {
        return new CPathLt(lt, label1Term, label2Term, cause);
    }

    @Override public <R> R match(Cases<R> cases) {
        return cases.casePathLt(this);
    }

    @Override public <R, E extends Throwable> R matchOrThrow(CheckedCases<R, E> cases) throws E {
        return cases.casePathLt(this);
    }

    @Override public CPathLt apply(ISubstitution.Immutable subst) {
        return new CPathLt(lt, subst.apply(label1Term), subst.apply(label2Term), cause);
    }

    @Override public String toString(TermFormatter termToString) {
        final StringBuilder sb = new StringBuilder();
        sb.append("pathLt[");
        sb.append(lt);
        sb.append("](");
        sb.append(termToString.format(label1Term));
        sb.append(", ");
        sb.append(termToString.format(label2Term));
        sb.append(")");
        return sb.toString();
    }

    @Override public String toString() {
        return toString(ITerm::toString);
    }

}