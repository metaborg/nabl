package mb.statix.solver.query;

import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.substitution.ISubstitution;
import mb.nabl2.terms.unification.IUnifier;
import mb.nabl2.terms.unification.PersistentUnifier;
import mb.statix.scopegraph.reference.DataWF;
import mb.statix.scopegraph.reference.LabelWF;
import mb.statix.solver.Completeness;
import mb.statix.solver.IDebugContext;
import mb.statix.solver.State;
import mb.statix.spec.Lambda;

public class QueryFilter implements IQueryFilter {

    private final Lambda pathConstraint;
    private final Lambda dataConstraint;

    public QueryFilter(Lambda pathConstraint, Lambda dataConstraint) {
        this.pathConstraint = pathConstraint;
        this.dataConstraint = dataConstraint;
    }

    public IQueryFilter apply(ISubstitution.Immutable subst) {
        return new QueryFilter(pathConstraint.apply(subst), dataConstraint.apply(subst));
    }

    public LabelWF<ITerm> getLabelWF(State state, Completeness completeness, IDebugContext debug) {
        return new ConstraintLabelWF(pathConstraint, state, completeness, debug);
    }

    public DataWF<ITerm> getDataWF(State state, Completeness completeness, IDebugContext debug) {
        return new ConstraintDataWF(dataConstraint, state, completeness, debug);
    }

    @Override public String toString(IUnifier unifier) {
        final StringBuilder sb = new StringBuilder();
        sb.append("filter ");
        sb.append(pathConstraint.toString(unifier));
        sb.append(" and ");
        sb.append(dataConstraint.toString(unifier));
        return sb.toString();
    }

    @Override public String toString() {
        return toString(PersistentUnifier.Immutable.of());
    }

}