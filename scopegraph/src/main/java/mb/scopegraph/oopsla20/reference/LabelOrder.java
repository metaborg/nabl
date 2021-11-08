package mb.scopegraph.oopsla20.reference;

/**
 * Provides an ordering on labels.
 *
 * @param <L> the type of labels
 */
public interface LabelOrder<L> {

    /**
     * Gets whether one label is smaller than another label.
     *
     * @param l1 the label to check whether it is the smaller one
     * @param l2 the label to check whether it is not the smaller one
     * @return {@code true} when {@code l1} is smaller than {@code l2}; otherwise, {@code false}
     */
    boolean lt(EdgeOrData<L> l1, EdgeOrData<L> l2) throws ResolutionException, InterruptedException;

    /**
     * Provides no label ordering.
     *
     * @param <L> the type of labels
     * @return a label order where no labels are smaller than other labels
     */
    static <L> LabelOrder<L> NONE() {
        return (l1, l2) -> false;
    }

}