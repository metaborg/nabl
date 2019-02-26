package mb.statix.scopegraph.path;

/**
 * @param <V>
 *      the type of scopes
 * @param <L>
 *      the type of labels
 */
public interface IScopePath<V, L>
        extends IPath<V, L>, Iterable<IStep<V, L>> {

    V getSource();

    V getTarget();

    default boolean isEmpty() {
        return size() == 0;
    }
    
    int size();

    String toString(boolean includeSource, boolean includeTarget);

}