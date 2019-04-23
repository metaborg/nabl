package mb.statix.scopegraph.path;

import java.util.List;
import java.util.Optional;

public interface IResolutionPath<S, L, D> extends IPath<S, L> {

    IScopePath<S, L> getPath();

    Optional<L> getRelation();

    List<D> getDatum();

}