package mb.statix.scopegraph;

import java.util.Collection;

import mb.statix.scopegraph.path.IResolutionPath;

public interface INameResolution<S extends D, L, D> {

    Collection<IResolutionPath<S, L, D>> resolve(S scope) throws Exception;

}