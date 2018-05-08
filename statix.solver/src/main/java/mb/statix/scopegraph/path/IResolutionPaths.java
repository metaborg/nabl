package mb.statix.scopegraph.path;

import java.util.Set;


public interface IResolutionPaths<S, L, R, O> {

    IResolutionPaths<S, L, R, O> inverse();

    Set<IResolutionPath<S, L, R, O>> get(O occurrence);

}