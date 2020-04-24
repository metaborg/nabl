package mb.statix.solver.persistent.query;

import mb.nabl2.regexp.IRegExpMatcher;
import mb.nabl2.relations.IRelation;
import mb.nabl2.terms.ITerm;
import mb.statix.scopegraph.reference.DataLeq;
import mb.statix.scopegraph.reference.DataWF;
import mb.statix.scopegraph.reference.LabelOrder;
import mb.statix.scopegraph.reference.LabelWF;
import mb.statix.solver.ConstraintContext;
import mb.statix.solver.IState;
import mb.statix.solver.query.IConstraintQueries;
import mb.statix.solver.query.RegExpLabelWF;
import mb.statix.solver.query.RelationLabelOrder;
import mb.statix.spec.Rule;
import mb.statix.spec.Spec;

public class ConstraintQueries implements IConstraintQueries {

    final Spec spec;
    final IState.Immutable state;
    final ConstraintContext params;

    public ConstraintQueries(Spec spec, IState.Immutable state, ConstraintContext params) {
        this.spec = spec;
        this.state = state;
        this.params = params;
    }

    @Override public LabelWF<ITerm> getLabelWF(IRegExpMatcher<ITerm> pathWf) throws InterruptedException {
        return RegExpLabelWF.of(pathWf);
    }

    @Override public DataWF<ITerm> getDataWF(Rule dataWf) {
        return new ConstraintDataWF(spec, dataWf, state, params::isComplete, params.debug());
    }

    @Override public LabelOrder<ITerm> getLabelOrder(IRelation<ITerm> labelOrd) throws InterruptedException {
        return new RelationLabelOrder(labelOrd);
    }

    @Override public DataLeq<ITerm> getDataEquiv(Rule dataLeq) {
        return new ConstraintDataLeq(spec, dataLeq, state, params::isComplete, params.debug());
    }

}
