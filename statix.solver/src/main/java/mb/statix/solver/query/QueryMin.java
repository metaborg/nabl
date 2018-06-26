package mb.statix.solver.query;

import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.substitution.ISubstitution;
import mb.nabl2.terms.unification.IUnifier;
import mb.nabl2.terms.unification.PersistentUnifier;
import mb.statix.scopegraph.reference.DataEquiv;
import mb.statix.scopegraph.reference.LabelOrder;
import mb.statix.solver.Completeness;
import mb.statix.solver.IDebugContext;
import mb.statix.solver.State;
import mb.statix.spec.Lambda;

public class QueryMin implements IQueryMin {

    private final Lambda pathConstraint;
    private final Lambda dataConstraint;

    public QueryMin(Lambda pathConstraint, Lambda dataConstraint) {
        this.pathConstraint = pathConstraint;
        this.dataConstraint = dataConstraint;
    }

    public IQueryMin apply(ISubstitution.Immutable subst) {
        return new QueryMin(pathConstraint.apply(subst), dataConstraint.apply(subst));
    }

    public LabelOrder<ITerm> getLabelOrder(State state, Completeness completeness, IDebugContext debug) {
        return new ConstraintLabelOrder(pathConstraint, state, completeness, debug);
    }

    public DataEquiv<ITerm> getDataEquiv(State state, Completeness completeness, IDebugContext debug) {
        return new ConstraintDataEquiv(dataConstraint, state, completeness, debug);
    }

    @Override public String toString(IUnifier unifier) {
        final StringBuilder sb = new StringBuilder();
        sb.append("min ");
        sb.append(pathConstraint.toString(unifier));
        sb.append(" and ");
        sb.append(dataConstraint.toString(unifier));
        return sb.toString();
    }

    @Override public String toString() {
        return toString(PersistentUnifier.Immutable.of());
    }

}