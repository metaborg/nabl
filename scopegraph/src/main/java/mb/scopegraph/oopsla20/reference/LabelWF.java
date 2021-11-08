package mb.scopegraph.oopsla20.reference;

import java.util.Optional;

/**
 * Well-formedness.
 *
 * @param <L> the type of labels
 */
public interface LabelWF<L> {

    /**
     * Attempts to perform a step in the well-formedness relation,
     * returning either the new well-formedness relation, or nothing if the step could not be taken.
     *
     * @param l the label for the step
     * @return either the new well-formedness relation, or nothing if it failed
     */
    Optional<LabelWF<L>> step(L l) throws ResolutionException, InterruptedException;

    /**
     * Whether the well-formedness is accepting.
     *
     * @return {@code true} when it is accepting;
     * otherwise, {@code false}
     */
    boolean accepting() throws ResolutionException, InterruptedException;

    /**
     * A well-formedness relation that accepts anything.
     *
     * @param <L> the type of labels
     * @return the well-formedness relation
     */
    static <L> LabelWF<L> ANY() {
        return new LabelWF<L>() {

            @Override public Optional<LabelWF<L>> step(@SuppressWarnings("unused") L l) {
                return Optional.of(this);
            }

            @Override public boolean accepting() {
                return true;
            }

        };
    }

}