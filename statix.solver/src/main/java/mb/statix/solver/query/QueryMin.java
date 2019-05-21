package mb.statix.solver.query;

import java.io.Serializable;

import mb.nabl2.relations.IRelation;
import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.substitution.ISubstitution;
import mb.nabl2.util.TermFormatter;
import mb.statix.spec.Rule;

public class QueryMin implements IQueryMin, Serializable {
    private static final long serialVersionUID = 1L;

    private final IRelation.Immutable<ITerm> labelOrd;
    private final Rule dataOrd;

    public QueryMin(IRelation.Immutable<ITerm> labelOrd, Rule dataConstraint) {
        this.labelOrd = labelOrd;
        this.dataOrd = dataConstraint;
    }

    @Override public IQueryMin apply(ISubstitution.Immutable subst) {
        return new QueryMin(labelOrd, dataOrd.apply(subst));
    }

    @Override public IRelation<ITerm> getLabelOrder() {
        return labelOrd;
    }

    @Override public Rule getDataEquiv() {
        return dataOrd;
    }

    @Override public String toString(TermFormatter termToString) {
        final StringBuilder sb = new StringBuilder();
        sb.append("min ");
        sb.append(labelOrd);
        sb.append(" and ");
        sb.append(dataOrd.toString(termToString));
        return sb.toString();
    }

    @Override public String toString() {
        return toString(ITerm::toString);
    }

}
