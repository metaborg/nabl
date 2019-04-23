package mb.statix.scopegraph.path;

import java.util.Set;


public interface IScopePaths<S, L> {

    IScopePaths<S, L> inverse();

    Set<IScopePath<S, L>> get(S scope);

}