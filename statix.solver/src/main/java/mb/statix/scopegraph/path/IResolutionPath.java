package mb.statix.scopegraph.path;

/**
 * Interface to represent a resolution path.
 *
 * @param <S>
 *      the type of scopes
 * @param <L>
 *      the type of labels
 * @param <D>
 *      the type of data
 */
public interface IResolutionPath<S, L, D> extends IPath<S, L> {

    IScopePath<S, L> getPath();

    L getLabel();

    D getDatum();

}