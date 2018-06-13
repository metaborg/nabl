package mb.statix.solver.query;

import org.metaborg.util.functions.Function1;

import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.unification.IUnifier;
import mb.statix.scopegraph.reference.DataWF;
import mb.statix.scopegraph.reference.LabelWF;
import mb.statix.solver.Completeness;
import mb.statix.solver.IDebugContext;
import mb.statix.solver.State;

public class QueryFilter implements IQueryFilter {

    private final String pathConstraint;
    private final String dataConstraint;

    public QueryFilter(String pathConstraint, String dataConstraint) {
        this.pathConstraint = pathConstraint;
        this.dataConstraint = dataConstraint;
    }

    public IQueryFilter apply(Function1<ITerm, ITerm> map) {
        return this;
    }

    public LabelWF<ITerm> getLabelWF(State state, Completeness completeness, IDebugContext debug) {
        return new ConstraintLabelWF(pathConstraint, state, completeness, debug);
    }

    public DataWF<ITerm> getDataWF(State state, Completeness completeness, IDebugContext debug) {
        return new ConstraintDataWF(dataConstraint, state, completeness, debug);
    }

    @Override public String toString(IUnifier unifier) {
        return toString();
    }

    @Override public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("filter ");
        sb.append(pathConstraint);
        sb.append(" and ");
        sb.append(dataConstraint);
        return sb.toString();
    }

}