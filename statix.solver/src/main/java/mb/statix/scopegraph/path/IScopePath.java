package mb.statix.scopegraph.path;

public interface IScopePath<V, L>
        extends IPath<V, L>, Iterable<IStep<V, L>> {

    V getSource();

    V getTarget();

    int size();

    String toString(boolean includeSource, boolean includeTarget);

}