package mb.statix.solver.query;

import mb.nabl2.terms.ITerm;
import mb.scopegraph.oopsla20.reference.DataLeq;
import mb.scopegraph.oopsla20.reference.DataWF;
import mb.scopegraph.oopsla20.reference.EdgeOrData;
import mb.scopegraph.oopsla20.reference.LabelOrder;
import mb.scopegraph.oopsla20.reference.LabelWF;
import mb.scopegraph.regexp.IRegExpMatcher;
import mb.scopegraph.relations.IRelation;
import mb.statix.spec.Rule;

public interface IConstraintQueries {

    LabelWF<ITerm> getLabelWF(IRegExpMatcher<ITerm> labelWf) throws InterruptedException;

    DataWF<ITerm> getDataWF(Rule dataWf) throws InterruptedException;

    LabelOrder<ITerm> getLabelOrder(IRelation<EdgeOrData<ITerm>> labelOrder) throws InterruptedException;

    DataLeq<ITerm> getDataEquiv(Rule dataEquiv) throws InterruptedException;

}