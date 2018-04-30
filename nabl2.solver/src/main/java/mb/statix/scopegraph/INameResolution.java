package mb.statix.scopegraph;

import java.util.Set;

import mb.statix.scopegraph.path.IResolutionPath;

public interface INameResolution<S, L, R, O> {

    Set<IResolutionPath<S, L, R, O>> resolve(S scope) throws Exception;

}