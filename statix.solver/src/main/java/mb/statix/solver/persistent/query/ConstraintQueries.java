package mb.statix.solver.persistent.query;

import mb.nabl2.terms.ITerm;
import mb.scopegraph.oopsla20.reference.DataLeq;
import mb.scopegraph.oopsla20.reference.DataWF;
import mb.scopegraph.oopsla20.reference.EdgeOrData;
import mb.scopegraph.oopsla20.reference.LabelOrder;
import mb.scopegraph.oopsla20.reference.LabelWF;
import mb.scopegraph.oopsla20.reference.RegExpLabelWF;
import mb.scopegraph.oopsla20.reference.RelationLabelOrder;
import mb.scopegraph.regexp.IRegExpMatcher;
import mb.scopegraph.relations.IRelation;
import mb.statix.solver.IState;
import mb.statix.solver.completeness.ICompleteness;
import mb.statix.solver.completeness.IsComplete;
import mb.statix.solver.query.IConstraintQueries;
import mb.statix.spec.Rule;
import mb.statix.spec.Spec;

public class ConstraintQueries implements IConstraintQueries {

    private final Spec spec;
    private final IState.Immutable state;
    private final IsComplete isComplete;

    public ConstraintQueries(Spec spec, IState.Immutable state, IsComplete isComplete) {
        this.spec = spec;
        this.state = state;
        this.isComplete = isComplete;
    }

    @Override public LabelWF<ITerm> getLabelWF(IRegExpMatcher<ITerm> pathWf) throws InterruptedException {
        return RegExpLabelWF.of(pathWf);
    }

    @Override public DataWF<ITerm> getDataWF(Rule dataWf) {
        return new ConstraintDataWF(spec, state, isComplete, dataWf);
    }

    @Override public LabelOrder<ITerm> getLabelOrder(IRelation<EdgeOrData<ITerm>> labelOrd)
            throws InterruptedException {
        return new RelationLabelOrder<>(labelOrd);
    }

    @Override public DataLeq<ITerm> getDataEquiv(Rule dataLeq) {
        return new ConstraintDataLeq(spec, state, isComplete, dataLeq);
    }

}
