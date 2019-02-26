package mb.statix.taico.scopegraph;

import java.util.Set;

import mb.statix.scopegraph.path.IResolutionPath;
import mb.statix.taico.util.IOwnable;

/**
 * Interface to represent a particular name resolution algorithm.
 *
 * @param <S>
 *      the type of scopes
 * @param <V>
 *      the type of data
 * @param <L>
 *      the type of edges
 * @param <R>
 *      the type of relations
 */
public interface IMNameResolution<S extends IOwnable, V, L, R> {

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
    Set<IResolutionPath<V, L, R>> resolve(S scope) throws Exception;

}