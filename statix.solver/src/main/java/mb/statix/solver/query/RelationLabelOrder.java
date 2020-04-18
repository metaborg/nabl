package mb.statix.solver.query;

import mb.nabl2.relations.IRelation;
import mb.nabl2.terms.ITerm;
import mb.statix.scopegraph.reference.EdgeOrData;
import mb.statix.scopegraph.reference.LabelOrder;
import mb.statix.scopegraph.reference.ResolutionException;

public class RelationLabelOrder implements LabelOrder<ITerm> {

    private final IRelation<EdgeOrData<ITerm>> labelOrd;

    public RelationLabelOrder(IRelation<EdgeOrData<ITerm>> labelOrd) {
        this.labelOrd = labelOrd;
    }

    @Override public boolean lt(EdgeOrData<ITerm> l1, EdgeOrData<ITerm> l2) throws ResolutionException, InterruptedException {
        return labelOrd.contains(l1, l2);
    }

}