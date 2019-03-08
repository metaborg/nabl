package mb.statix.taico.solver.query;

import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.substitution.ISubstitution;
import mb.nabl2.util.TermFormatter;
import mb.statix.scopegraph.reference.DataLeq;
import mb.statix.scopegraph.reference.LabelOrder;
import mb.statix.solver.log.IDebugContext;
import mb.statix.spec.Rule;
import mb.statix.taico.solver.MCompleteness;
import mb.statix.taico.solver.MState;
import mb.statix.taico.solver.query.IMQueryMin;

public class MQueryMin implements IMQueryMin {

    private final Rule pathConstraint;
    private final Rule dataConstraint;

    public MQueryMin(Rule pathConstraint, Rule dataConstraint) {
        this.pathConstraint = pathConstraint;
        this.dataConstraint = dataConstraint;
    }

    public IMQueryMin apply(ISubstitution.Immutable subst) {
        return new MQueryMin(pathConstraint.apply(subst), dataConstraint.apply(subst));
    }

    public LabelOrder<ITerm> getLabelOrder(MState state, MCompleteness completeness, IDebugContext debug) {
        return new MConstraintLabelOrder(pathConstraint, state.copy(), completeness.copy(), debug);
    }

    public DataLeq<ITerm> getDataEquiv(MState state, MCompleteness completeness, IDebugContext debug) {
        return new MConstraintDataLeq(dataConstraint, state.copy(), completeness.copy(), debug);
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