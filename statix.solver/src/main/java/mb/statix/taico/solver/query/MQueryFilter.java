package mb.statix.taico.solver.query;

import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.substitution.ISubstitution;
import mb.nabl2.util.TermFormatter;
import mb.statix.scopegraph.reference.DataWF;
import mb.statix.scopegraph.reference.LabelWF;
import mb.statix.solver.log.IDebugContext;
import mb.statix.spec.Rule;
import mb.statix.taico.solver.MCompleteness;
import mb.statix.taico.solver.MState;

/**
 * Class to represent query filters.
 * 
 * <pre>filter &lt;path&gt; and &lt;data&gt;</pre>
 */
public class MQueryFilter implements IMQueryFilter {

    private final Rule pathConstraint;
    private final Rule dataConstraint;

    public MQueryFilter(Rule pathConstraint, Rule dataConstraint) {
        this.pathConstraint = pathConstraint;
        this.dataConstraint = dataConstraint;
    }

    public IMQueryFilter apply(ISubstitution.Immutable subst) {
        return new MQueryFilter(pathConstraint.apply(subst), dataConstraint.apply(subst));
    }

    public LabelWF<ITerm> getLabelWF(MState state, MCompleteness completeness, IDebugContext debug) {
        return MConstraintLabelWF.of(pathConstraint, state, completeness, debug);
    }

    public DataWF<ITerm> getDataWF(MState state, MCompleteness completeness, IDebugContext debug) {
        return new MConstraintDataWF(dataConstraint, state, completeness, debug);
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

}