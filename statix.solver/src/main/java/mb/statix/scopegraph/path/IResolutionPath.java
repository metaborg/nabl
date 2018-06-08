package mb.statix.scopegraph.path;

import java.util.List;
import java.util.Optional;

public interface IResolutionPath<V, L, R> extends IPath<V, L> {

    IScopePath<V, L> getPath();

    Optional<R> getRelation();

    List<V> getDatum();

}