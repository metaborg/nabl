package mb.statix.solver.persistent.query;

import mb.nabl2.terms.ITerm;
import mb.statix.scopegraph.reference.DataLeq;
import mb.statix.scopegraph.reference.DataWF;
import mb.statix.scopegraph.reference.LabelOrder;
import mb.statix.scopegraph.reference.LabelWF;
import mb.statix.solver.ConstraintContext;
import mb.statix.solver.persistent.State;
import mb.statix.solver.query.IConstraintQueries;
import mb.statix.spec.Rule;

public class ConstraintQueries implements IConstraintQueries {

    final State state;
    final ConstraintContext params;

    public ConstraintQueries(State state, ConstraintContext params) {
        this.state = state;
        this.params = params;
    }

    @Override public LabelWF<ITerm> getLabelWF(Rule labelWf) throws InterruptedException {
        return ConstraintLabelWF.of(labelWf, state, params::isComplete, params.debug());
    }

    @Override public DataWF<ITerm> getDataWF(Rule dataWf) {
        return new ConstraintDataWF(dataWf, state, params::isComplete, params.debug());
    }

    @Override public LabelOrder<ITerm> getLabelOrder(Rule labelOrder) {
        return new ConstraintLabelOrder(labelOrder, state, params::isComplete, params.debug());
    }

    @Override public DataLeq<ITerm> getDataEquiv(Rule dataLeq) {
        return new ConstraintDataLeq(dataLeq, state, params::isComplete, params.debug());
    }

}
