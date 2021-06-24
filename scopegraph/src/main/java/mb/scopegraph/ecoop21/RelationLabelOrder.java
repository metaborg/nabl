package mb.scopegraph.ecoop21;

import java.io.Serializable;

import mb.scopegraph.oopsla20.reference.EdgeOrData;
import mb.scopegraph.relations.IRelation;

public class RelationLabelOrder<L> implements LabelOrder<L>, Serializable {

    private static final long serialVersionUID = 42L;

    private final IRelation<EdgeOrData<L>> labelOrd;

    public RelationLabelOrder(IRelation<EdgeOrData<L>> labelOrd) {
        this.labelOrd = labelOrd;
    }

    @Override public boolean lt(EdgeOrData<L> l1, EdgeOrData<L> l2) {
        return labelOrd.contains(l1, l2);
    }

    @Override public String toString() {
        return labelOrd.toString();
    }

}