package mb.statix.taico.solver.query;

import java.io.Serializable;

import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.substitution.ISubstitution;
import mb.nabl2.util.TermFormatter;
import mb.statix.scopegraph.reference.DataWF;
import mb.statix.scopegraph.reference.LabelWF;
import mb.statix.solver.log.IDebugContext;
import mb.statix.spec.IRule;
import mb.statix.taico.solver.ICompleteness;
import mb.statix.taico.solver.IMState;

/**
 * Class to represent query filters.
 * 
 * <pre>filter &lt;path&gt; and &lt;data&gt;</pre>
 */
public class MQueryFilter implements IMQueryFilter, Serializable {
    private static final long serialVersionUID = 1L;

    private final IRule pathConstraint;
    private final IRule dataConstraint;

    public MQueryFilter(IRule pathConstraint, IRule dataConstraint) {
        this.pathConstraint = pathConstraint;
        this.dataConstraint = dataConstraint;
    }

    @Override public IMQueryFilter apply(ISubstitution.Immutable subst) {
        return new MQueryFilter(pathConstraint.apply(subst), dataConstraint.apply(subst));
    }

    @Override public LabelWF<ITerm> getLabelWF(IMState state, ICompleteness isComplete, IDebugContext debug) {
        return MConstraintLabelWF.of(pathConstraint, state, isComplete, debug);
    }

    @Override public DataWF<ITerm> getDataWF(IMState state, ICompleteness isComplete, IDebugContext debug) {
        return new MConstraintDataWF(dataConstraint, state, isComplete, debug);
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