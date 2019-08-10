package mb.statix.modular.solver.query;

import mb.nabl2.regexp.IRegExpMatcher;
import mb.nabl2.relations.IRelation;
import mb.nabl2.terms.ITerm;
import mb.statix.modular.solver.MConstraintContext;
import mb.statix.modular.solver.state.IMState;
import mb.statix.scopegraph.reference.DataLeq;
import mb.statix.scopegraph.reference.DataWF;
import mb.statix.scopegraph.reference.LabelOrder;
import mb.statix.scopegraph.reference.LabelWF;
import mb.statix.solver.query.IConstraintQueries;
import mb.statix.solver.query.RegExpLabelWF;
import mb.statix.solver.query.RelationLabelOrder;
import mb.statix.spec.IRule;

public class MConstraintQueries implements IConstraintQueries {

    final IMState state;
    final MConstraintContext params;

    public MConstraintQueries(IMState state, MConstraintContext params) {
        this.state = state;
        this.params = params;
    }

    @Override public LabelWF<ITerm> getLabelWF(IRegExpMatcher<ITerm> pathWf) throws InterruptedException {
        return RegExpLabelWF.of(pathWf);
    }

    @Override public DataWF<ITerm> getDataWF(IRule dataWf) {
        return new MConstraintDataWF(dataWf, state, params::isComplete, params.debug());
    }

    @Override public LabelOrder<ITerm> getLabelOrder(IRelation<ITerm> labelOrd) throws InterruptedException {
        return new RelationLabelOrder(labelOrd);
    }

    @Override public DataLeq<ITerm> getDataEquiv(IRule dataLeq) {
        return new MConstraintDataLeq(dataLeq, state, params::isComplete, params.debug());
    }

}
