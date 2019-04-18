package mb.statix.taico.solver.query;

import java.io.Serializable;

import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.substitution.ISubstitution;
import mb.nabl2.util.TermFormatter;
import mb.statix.scopegraph.reference.DataLeq;
import mb.statix.scopegraph.reference.LabelOrder;
import mb.statix.solver.log.IDebugContext;
import mb.statix.spec.IRule;
import mb.statix.taico.solver.ICompleteness;
import mb.statix.taico.solver.IMState;

public class MQueryMin implements IMQueryMin, Serializable {
    private static final long serialVersionUID = 1L;

    private final IRule pathConstraint;
    private final IRule dataConstraint;

    public MQueryMin(IRule pathConstraint, IRule dataConstraint) {
        this.pathConstraint = pathConstraint;
        this.dataConstraint = dataConstraint;
    }

    @Override public IMQueryMin apply(ISubstitution.Immutable subst) {
        return new MQueryMin(pathConstraint.apply(subst), dataConstraint.apply(subst));
    }

    @Override public LabelOrder<ITerm> getLabelOrder(IMState state, ICompleteness isComplete, IDebugContext debug) {
        return new MConstraintLabelOrder(pathConstraint, state.delegate(), isComplete, debug);
    }

    @Override public DataLeq<ITerm> getDataEquiv(IMState state, ICompleteness isComplete, IDebugContext debug) {
        return new MConstraintDataLeq(dataConstraint, state.delegate(), isComplete, debug);
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