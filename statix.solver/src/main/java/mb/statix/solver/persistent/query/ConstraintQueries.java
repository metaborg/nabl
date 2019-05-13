package mb.statix.solver.persistent.query;

import mb.nabl2.regexp.IRegExpMatcher;
import mb.nabl2.relations.IRelation;
import mb.nabl2.terms.ITerm;
import mb.statix.scopegraph.reference.DataLeq;
import mb.statix.scopegraph.reference.DataWF;
import mb.statix.scopegraph.reference.LabelOrder;
import mb.statix.scopegraph.reference.LabelWF;
import mb.statix.solver.ConstraintContext;
import mb.statix.solver.persistent.State;
import mb.statix.solver.query.IConstraintQueries;
import mb.statix.solver.query.RegExpLabelWF;
import mb.statix.solver.query.RelationLabelOrder;
import mb.statix.spec.Rule;

public class ConstraintQueries implements IConstraintQueries {

    final State state;
    final ConstraintContext params;

    public ConstraintQueries(State state, ConstraintContext params) {
        this.state = state;
        this.params = params;
    }

    @Override public LabelWF<ITerm> getLabelWF(IRegExpMatcher<ITerm> pathWf) throws InterruptedException {
        return RegExpLabelWF.of(pathWf);
    }

    @Override public DataWF<ITerm> getDataWF(Rule dataWf) {
        return new ConstraintDataWF(dataWf, state, params::isComplete, params.debug());
    }

    @Override public LabelOrder<ITerm> getLabelOrder(IRelation<ITerm> labelOrd) throws InterruptedException {
        return new RelationLabelOrder(labelOrd);
    }

    @Override public DataLeq<ITerm> getDataEquiv(Rule dataLeq) {
        return new ConstraintDataLeq(dataLeq, state, params::isComplete, params.debug());
    }

}
