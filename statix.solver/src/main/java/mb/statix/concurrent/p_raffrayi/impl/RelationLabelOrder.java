package mb.statix.concurrent.p_raffrayi.impl;

import mb.nabl2.relations.IRelation;
import mb.nabl2.terms.ITerm;
import mb.statix.concurrent.p_raffrayi.nameresolution.LabelOrder;
import mb.statix.scopegraph.reference.EdgeOrData;

public class RelationLabelOrder implements LabelOrder<ITerm> {

    private final IRelation<EdgeOrData<ITerm>> labelOrd;

    public RelationLabelOrder(IRelation<EdgeOrData<ITerm>> labelOrd) {
        this.labelOrd = labelOrd;
    }

    @Override public boolean lt(EdgeOrData<ITerm> l1, EdgeOrData<ITerm> l2) {
        return labelOrd.contains(l1, l2);
    }

    @Override public String toString() {
        return labelOrd.toString();
    }

}