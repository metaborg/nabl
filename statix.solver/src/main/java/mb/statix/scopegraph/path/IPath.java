package mb.statix.scopegraph.path;

import io.usethesource.capsule.Set;
import mb.nabl2.util.collections.PSequence;

/**
 * Interface to represent a path.
 *
 * @param <V>
 *      the type of scopes
 * @param <L>
 *      the type of labels
 */
public interface IPath<V, L> {

    PSequence<V> scopes();

    Set.Immutable<V> scopeSet();

    PSequence<L> labels();

}