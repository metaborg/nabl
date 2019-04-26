package mb.statix.solver.query;

import mb.nabl2.terms.ITerm;
import mb.statix.scopegraph.reference.DataLeq;
import mb.statix.scopegraph.reference.DataWF;
import mb.statix.scopegraph.reference.LabelOrder;
import mb.statix.scopegraph.reference.LabelWF;
import mb.statix.spec.Rule;

public interface IConstraintQueries {

    LabelWF<ITerm> getLabelWF(Rule labelWf) throws InterruptedException;

    DataWF<ITerm> getDataWF(Rule dataWf) throws InterruptedException;

    LabelOrder<ITerm> getLabelOrder(Rule labelOrder) throws InterruptedException;

    DataLeq<ITerm> getDataEquiv(Rule dataEquiv) throws InterruptedException;

}