package mb.statix.solver.query;

import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.substitution.ISubstitution;
import mb.nabl2.util.TermFormatter;
import mb.statix.scopegraph.reference.DataLeq;
import mb.statix.scopegraph.reference.LabelOrder;
import mb.statix.solver.Completeness;
import mb.statix.solver.State;
import mb.statix.solver.log.IDebugContext;
import mb.statix.spec.IRule;
import mb.statix.taico.solver.query.IMQueryMin;
import mb.statix.taico.solver.query.MQueryMin;

public class QueryMin implements IQueryMin {

    private final IRule pathConstraint;
    private final IRule dataConstraint;

    public QueryMin(IRule pathConstraint, IRule dataConstraint) {
        this.pathConstraint = pathConstraint;
        this.dataConstraint = dataConstraint;
    }

    public IQueryMin apply(ISubstitution.Immutable subst) {
        return new QueryMin(pathConstraint.apply(subst), dataConstraint.apply(subst));
    }

    public LabelOrder<ITerm> getLabelOrder(State state, Completeness completeness, IDebugContext debug) {
        return new ConstraintLabelOrder(pathConstraint, state, completeness, debug);
    }

    public DataLeq<ITerm> getDataEquiv(State state, Completeness completeness, IDebugContext debug) {
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
    
    @Override
    public IMQueryMin toMutable() {
        return new MQueryMin(pathConstraint, dataConstraint);
    }

}