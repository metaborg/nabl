package mb.statix.solver.persistent.query;

import org.metaborg.util.task.ICancel;
import org.metaborg.util.task.IProgress;

import mb.nabl2.regexp.IRegExpMatcher;
import mb.nabl2.relations.IRelation;
import mb.nabl2.terms.ITerm;
import mb.statix.scopegraph.reference.DataLeq;
import mb.statix.scopegraph.reference.DataWF;
import mb.statix.scopegraph.reference.EdgeOrData;
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

    private final Spec spec;
    private final IState.Immutable state;
    private final ConstraintContext params;
    private final IProgress progress;
    private final ICancel cancel;

    public ConstraintQueries(Spec spec, IState.Immutable state, ConstraintContext params, IProgress progress,
            ICancel cancel) {
        this.spec = spec;
        this.state = state;
        this.params = params;
        this.progress = progress;
        this.cancel = cancel;
    }

    @Override public LabelWF<ITerm> getLabelWF(IRegExpMatcher<ITerm> pathWf) throws InterruptedException {
        return RegExpLabelWF.of(pathWf);
    }

    @Override public DataWF<ITerm> getDataWF(Rule dataWf) {
        return new ConstraintDataWF(spec, dataWf, state, params::isComplete, params.debug(), progress, cancel);
    }

    @Override public LabelOrder<ITerm> getLabelOrder(IRelation<EdgeOrData<ITerm>> labelOrd)
            throws InterruptedException {
        return new RelationLabelOrder(labelOrd);
    }

    @Override public DataLeq<ITerm> getDataEquiv(Rule dataLeq) {
        return new ConstraintDataLeq(spec, dataLeq, state, params::isComplete, params.debug(), progress, cancel);
    }

}
