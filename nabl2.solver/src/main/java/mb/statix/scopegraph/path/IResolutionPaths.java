package mb.statix.scopegraph.path;

import java.util.Set;


public interface IResolutionPaths<S, L, O> {

    IResolutionPaths<S, L, O> inverse();

    Set<IResolutionPath<S, L, O>> get(O occurrence);

}