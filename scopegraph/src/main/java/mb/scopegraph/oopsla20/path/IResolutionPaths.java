package mb.scopegraph.oopsla20.path;

import java.util.Set;


public interface IResolutionPaths<S, L, D> {

    IResolutionPaths<S, L, D> inverse();

    Set<IResolutionPath<S, L, D>> get(D datum);

}