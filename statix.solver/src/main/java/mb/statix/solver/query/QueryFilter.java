package mb.statix.solver.query;

import java.io.Serializable;

import org.metaborg.util.functions.Predicate3;

import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.substitution.ISubstitution;
import mb.nabl2.util.TermFormatter;
import mb.statix.scopegraph.reference.DataWF;
import mb.statix.scopegraph.reference.LabelWF;
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
public class QueryFilter implements IQueryFilter, Serializable {
    private static final long serialVersionUID = 1L;

    private final IRule pathConstraint;
    private final IRule dataConstraint;

    public QueryFilter(IRule pathConstraint, IRule dataConstraint) {
        this.pathConstraint = pathConstraint;
        this.dataConstraint = dataConstraint;
    }

    @Override public IQueryFilter apply(ISubstitution.Immutable subst) {
        return new QueryFilter(pathConstraint.apply(subst), dataConstraint.apply(subst));
    }

    @Override public LabelWF<ITerm> getLabelWF(State state, Predicate3<ITerm, ITerm, State> isComplete,
            IDebugContext debug) {
        return ConstraintLabelWF.of(pathConstraint, state, isComplete, debug);
    }

    @Override public DataWF<ITerm> getDataWF(State state, Predicate3<ITerm, ITerm, State> isComplete,
            IDebugContext debug) {
        return new ConstraintDataWF(dataConstraint, state, isComplete, debug);
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