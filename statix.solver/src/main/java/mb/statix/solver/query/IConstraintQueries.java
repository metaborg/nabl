package mb.statix.solver.query;

import mb.nabl2.regexp.IRegExpMatcher;
import mb.nabl2.relations.IRelation;
import mb.nabl2.terms.ITerm;
import mb.statix.scopegraph.reference.DataLeq;
import mb.statix.scopegraph.reference.DataWF;
import mb.statix.scopegraph.reference.EdgeOrData;
import mb.statix.scopegraph.reference.LabelOrder;
import mb.statix.scopegraph.reference.LabelWF;
import mb.statix.spec.Rule;

public interface IConstraintQueries {

    LabelWF<ITerm> getLabelWF(IRegExpMatcher<ITerm> labelWf) throws InterruptedException;

    DataWF<ITerm> getDataWF(Rule dataWf) throws InterruptedException;

    LabelOrder<ITerm> getLabelOrder(IRelation<EdgeOrData<ITerm>> labelOrder) throws InterruptedException;

    DataLeq<ITerm> getDataEquiv(Rule dataEquiv) throws InterruptedException;

}