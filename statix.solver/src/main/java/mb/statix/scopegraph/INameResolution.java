package mb.statix.scopegraph;

import java.util.Set;

import mb.statix.scopegraph.path.IResolutionPath;

public interface INameResolution<V, L, R> {

    Set<IResolutionPath<V, L, R>> resolve(V scope) throws Exception;

}