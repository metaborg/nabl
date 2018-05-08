package mb.statix.scopegraph.path;

import java.util.Set;


public interface IScopePaths<S, L, O> {

    IScopePaths<S, L, O> inverse();

    Set<IScopePath<S, L, O>> get(S scope);

}