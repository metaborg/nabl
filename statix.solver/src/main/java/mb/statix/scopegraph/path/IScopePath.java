package mb.statix.scopegraph.path;

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