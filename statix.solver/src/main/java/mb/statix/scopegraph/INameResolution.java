package mb.statix.scopegraph;

import java.util.Set;

import mb.statix.scopegraph.path.IResolutionPath;

public interface INameResolution<S extends D, L, D> {

    Set<IResolutionPath<S, L, D>> resolve(S scope) throws Exception;

}