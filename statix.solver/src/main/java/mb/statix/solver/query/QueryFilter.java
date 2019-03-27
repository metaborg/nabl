package mb.statix.solver.query;

import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.substitution.ISubstitution;
import mb.nabl2.util.TermFormatter;
import mb.statix.scopegraph.reference.DataWF;
import mb.statix.scopegraph.reference.LabelWF;
import mb.statix.solver.Completeness;
import mb.statix.solver.State;
import mb.statix.solver.log.IDebugContext;
import mb.statix.spec.IRule;
import mb.statix.taico.solver.query.IMQueryFilter;
import mb.statix.taico.solver.query.MQueryFilter;

/**
 * Class to represent query filters.
 * 
 * <pre>filter &lt;path&gt; and &lt;data&gt;</pre>
 */
public class QueryFilter implements IQueryFilter {

    private final IRule pathConstraint;
    private final IRule dataConstraint;

    public QueryFilter(IRule pathConstraint, IRule dataConstraint) {
        this.pathConstraint = pathConstraint;
        this.dataConstraint = dataConstraint;
    }

    public IQueryFilter apply(ISubstitution.Immutable subst) {
        return new QueryFilter(pathConstraint.apply(subst), dataConstraint.apply(subst));
    }

    public LabelWF<ITerm> getLabelWF(State state, Completeness completeness, IDebugContext debug) {
        return ConstraintLabelWF.of(pathConstraint, state, completeness, debug);
    }

    public DataWF<ITerm> getDataWF(State state, Completeness completeness, IDebugContext debug) {
        return new ConstraintDataWF(dataConstraint, state, completeness, debug);
    }

    @Override public String toString(TermFormatter termToString) {
        final StringBuilder sb = new StringBuilder();
        sb.append("filter ");
        sb.append(pathConstraint.toString(termToString));
        sb.append(" and ");
        sb.append(dataConstraint.toString(termToString));
        return sb.toString();
    }

    @Override public String toString() {
        return toString(ITerm::toString);
    }

    @Override
    public IMQueryFilter toMutable() {
        return new MQueryFilter(pathConstraint, dataConstraint);
    }
}