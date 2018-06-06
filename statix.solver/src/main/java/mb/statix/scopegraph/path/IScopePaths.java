package mb.statix.scopegraph.path;

import java.util.Set;


public interface IScopePaths<V, L> {

    IScopePaths<V, L> inverse();

    Set<IScopePath<V, L>> get(V scope);

}