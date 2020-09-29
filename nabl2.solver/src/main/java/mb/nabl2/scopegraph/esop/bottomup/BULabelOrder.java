package mb.nabl2.scopegraph.esop.bottomup;

import mb.nabl2.relations.IRelation;

class BULabelOrder<L> {

    private final IRelation<L> order;

    public BULabelOrder(IRelation<L> order) {
        this.order = order;
    }

    public Integer test(L l1, L l2) {
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