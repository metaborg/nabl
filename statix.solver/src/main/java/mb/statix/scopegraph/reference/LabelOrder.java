package mb.statix.scopegraph.reference;

/**
 * Interface to represent label order.
 * 
 * @param <L>
 *      the type of labels
 */
public interface LabelOrder<L> {

    /**
     * @param l1
     *      the first label
     * @param l2
     *      the second label
     * 
     * @return
     *      if the first label takes precedence over the second label
     * 
     * @throws ResolutionException
     *      If the resolution cannot be completed for some reason.
     * @throws InterruptedException
     *      If the current thread is interrupted.
     */
    boolean lt(L l1, L l2) throws ResolutionException, InterruptedException;

    /**
     * @return
     *      label order where no label takes precedence over another label
     */
    static <L> LabelOrder<L> NONE() {
        return (l1, l2) -> false;
    }

}