package mb.statix.scopegraph.path;

/**
 * @param <S>
 *      the type of scopes
 * @param <L>
 *      the type of labels
 */
public interface IScopePath<S, L> extends IPath<S, L>, Iterable<IStep<S, L>> {

    S getSource();

    S getTarget();

    default boolean isEmpty() {
        return size() == 0;
    }

    int size();

    String toString(boolean includeSource, boolean includeTarget);

}