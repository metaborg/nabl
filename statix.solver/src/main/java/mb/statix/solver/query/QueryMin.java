package mb.statix.solver.query;

import java.io.Serializable;

import mb.nabl2.relations.IRelation;
import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.substitution.ISubstitution;
import mb.nabl2.util.TermFormatter;
import mb.statix.scopegraph.reference.DataLeq;
import mb.statix.scopegraph.reference.LabelOrder;
import mb.statix.solver.State;
import mb.statix.solver.completeness.IsComplete;
import mb.statix.solver.log.IDebugContext;
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

    @Override public LabelOrder<ITerm> getLabelOrder(State state, IsComplete isComplete, IDebugContext debug) {
        return new RelationLabelOrder(labelOrd);
    }

    @Override public DataLeq<ITerm> getDataEquiv(State state, IsComplete isComplete, IDebugContext debug) {
        return new ConstraintDataLeq(dataOrd, state, isComplete, debug);
    }

    @Override public String toString(TermFormatter termToString) {
        final StringBuilder sb = new StringBuilder();
        sb.append("min pathLt[");
        sb.append(labelOrd);
        sb.append("] and ");
        sb.append(dataOrd.toString(termToString));
        return sb.toString();
    }

    @Override public String toString() {
        return toString(ITerm::toString);
    }

}