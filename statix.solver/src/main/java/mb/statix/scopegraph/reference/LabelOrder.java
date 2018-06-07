package mb.statix.scopegraph.reference;

public interface LabelOrder<L> {

    /**
     * This predicate should be prefix-monotone. If p1 < p2, then p1.p1' < p1.p2' must hold.
     */
    boolean lt(L l1, L l2);

    static <L> LabelOrder<L> NONE() {
        return (l1, l2) -> false;
    }

}