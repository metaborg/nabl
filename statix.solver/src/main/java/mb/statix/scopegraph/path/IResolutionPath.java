package mb.statix.scopegraph.path;

import java.util.List;

public interface IResolutionPath<V, L, R> extends IPath<V, L> {

    IScopePath<V, L> getPath();

    R getRelation();

    List<V> getDatum();

}