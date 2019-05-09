package mb.statix.scopegraph.path;

import java.util.List;

public interface IResolutionPath<S, L, D> extends IPath<S, L> {

    IScopePath<S, L> getPath();

    L getLabel();

    List<D> getDatum();

}