package mb.statix.scopegraph.reference;

interface LabelOrder<L> {

    boolean lt(L l1, L l2);

    static <L> LabelOrder<L> NONE() {
        return new LabelOrder<L>() {

            public boolean lt(L l1, L l2) {
                return false;
            }

        };
    }

}