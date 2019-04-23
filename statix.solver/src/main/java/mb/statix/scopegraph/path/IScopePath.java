package mb.statix.scopegraph.path;

public interface IScopePath<S, L> extends IPath<S, L>, Iterable<IStep<S, L>> {

    S getSource();

    S getTarget();

    default boolean isEmpty() {
        return size() == 0;
    }

    int size();

    String toString(boolean includeSource, boolean includeTarget);

}