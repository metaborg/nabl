package mb.statix.scopegraph.path;

public interface IScopePath<S, L, O>
        extends IPath<S, L, O>, Iterable<IStep<S, L, O>> {

    S getSource();

    S getTarget();

    int size();

    String toString(boolean includeSource, boolean includeTarget);

}