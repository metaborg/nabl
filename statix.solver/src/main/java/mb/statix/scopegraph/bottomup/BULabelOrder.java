package mb.statix.scopegraph.bottomup;

import mb.nabl2.relations.IRelation;
import mb.statix.scopegraph.reference.EdgeOrData;

class BULabelOrder<L> {

    private final IRelation<EdgeOrData<L>> order;

    public BULabelOrder(IRelation<EdgeOrData<L>> order) {
        this.order = order;
    }

    public Integer test(EdgeOrData<L> l1, EdgeOrData<L> l2) {
        if(order.isEmpty()) {
            return null;
        }
        if(order.contains(l1, l2)) {
            return -1;
        }
        if(order.contains(l2, l1)) {
            return 1;
        }
        return 0;
    }

}