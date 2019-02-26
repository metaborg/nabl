package mb.statix.scopegraph.path;

import java.util.List;
import java.util.Optional;

/**
 * Interface to represent a resolution path.
 *
 * @param <V>
 *      the type of scopes and data
 * @param <L>
 *      the type of labels
 * @param <R>
 *      the type of relations
 */
public interface IResolutionPath<V, L, R> extends IPath<V, L> {

    IScopePath<V, L> getPath();

    Optional<R> getRelation();

    List<V> getDatum();

}