package mb.statix.concurrent.p_raffrayi.nameresolution;

import mb.statix.scopegraph.reference.EdgeOrData;

public interface LabelOrder<L> {

    boolean lt(EdgeOrData<L> l1, EdgeOrData<L> l2);

    static <L> LabelOrder<L> none() {
        return new LabelOrder<L>() {
            @SuppressWarnings("unused") @Override public boolean lt(EdgeOrData<L> l1, EdgeOrData<L> l2) {
                return false;
            }
        };
    }

}