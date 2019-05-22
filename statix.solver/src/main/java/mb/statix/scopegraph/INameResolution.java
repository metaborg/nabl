package mb.statix.scopegraph;

import java.util.Set;

import mb.statix.scopegraph.path.IResolutionPath;

/**
 * Interface to represent a particular name resolution.
 *
 * @param <S>
 *      the type of scopes
 * @param <L>
 *      the type of edges
 * @param <D>
 *      the type of data
 */
public interface INameResolution<S extends D, L, D> {

    /**
     * Attempts to resolve a name in the given scope.
     * 
     * @param scope
     *      the scope to start in
     * 
     * @return
     *      a set of all possible resolutions for this name in the given scope
     * 
     * @throws Exception
     *      If an error occurs during resolution.
     */
    Set<IResolutionPath<S, L, D>> resolve(S scope) throws Exception;

}