package mb.statix.scopegraph.reference;

public interface LabelOrder<L> {

    boolean lt(EdgeOrData<L> l1, EdgeOrData<L> l2) throws ResolutionException, InterruptedException;

    static <L> LabelOrder<L> NONE() {
        return (l1, l2) -> false;
    }

}