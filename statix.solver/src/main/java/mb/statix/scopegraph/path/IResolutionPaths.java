package mb.statix.scopegraph.path;

import java.util.Set;


public interface IResolutionPaths<V, L, R> {

    IResolutionPaths<V, L, R> inverse();

    Set<IResolutionPath<V, L, R>> get(V datum);

}