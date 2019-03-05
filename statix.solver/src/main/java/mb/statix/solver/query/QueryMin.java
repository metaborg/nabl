package mb.statix.solver.query;

import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.substitution.ISubstitution;
import mb.nabl2.util.TermFormatter;
import mb.statix.scopegraph.reference.DataLeq;
import mb.statix.scopegraph.reference.LabelOrder;
import mb.statix.solver.Completeness;
import mb.statix.solver.State;
import mb.statix.solver.log.IDebugContext;
import mb.statix.spec.Rule;

public class QueryMin implements IQueryMin {

    private final Rule pathConstraint;
    private final Rule dataConstraint;

    public QueryMin(Rule pathConstraint, Rule dataConstraint) {
        this.pathConstraint = pathConstraint;
        this.dataConstraint = dataConstraint;
    }

    @Override public IQueryMin apply(ISubstitution.Immutable subst) {
        return new QueryMin(pathConstraint.apply(subst), dataConstraint.apply(subst));
    }

    @Override public LabelOrder<ITerm> getLabelOrder(State state, Completeness completeness, IDebugContext debug) {
        return new ConstraintLabelOrder(pathConstraint, state, completeness, debug);
    }

    @Override public DataLeq<ITerm> getDataEquiv(State state, Completeness completeness, IDebugContext debug) {
        return new ConstraintDataLeq(dataConstraint, state, completeness, debug);
    }

    @Override public String toString(TermFormatter termToString) {
        final StringBuilder sb = new StringBuilder();
        sb.append("min ");
        sb.append(pathConstraint.toString(termToString));
        sb.append(" and ");
        sb.append(dataConstraint.toString(termToString));
        return sb.toString();
    }

    @Override public String toString() {
        return toString(ITerm::toString);
    }

}