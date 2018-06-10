package mb.statix.solver.query;

import org.metaborg.util.functions.Function1;

import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.unification.IUnifier;
import mb.statix.scopegraph.reference.DataEquiv;
import mb.statix.scopegraph.reference.LabelOrder;
import mb.statix.solver.Completeness;
import mb.statix.solver.IDebugContext;
import mb.statix.solver.State;

public class QueryMin implements IQueryMin {

    private final String pathConstraint;
    private final String dataConstraint;

    public QueryMin(String pathConstraint, String dataConstraint) {
        this.pathConstraint = pathConstraint;
        this.dataConstraint = dataConstraint;
    }

    public IQueryMin apply(Function1<ITerm, ITerm> map) {
        return this;
    }

    public LabelOrder<ITerm> getLabelOrder(State state, Completeness completeness, IDebugContext debug) {
        return new ConstraintLabelOrder(pathConstraint, state, completeness, debug);
    }

    public DataEquiv<ITerm> getDataEquiv(State state, Completeness completeness, IDebugContext debug) {
        return new ConstraintDataEquiv(dataConstraint, state, completeness, debug);
    }

    @Override public String toString(IUnifier unifier) {
        return toString();
    }

    @Override public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("min ");
        sb.append(pathConstraint);
        sb.append(" and ");
        sb.append(dataConstraint);
        return sb.toString();
    }

}