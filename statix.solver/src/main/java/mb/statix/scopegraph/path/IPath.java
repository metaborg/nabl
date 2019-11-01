package mb.statix.scopegraph.path;

import io.usethesource.capsule.Set;
import mb.nabl2.util.collections.PSequence;

/**
 * Interface to represent a path.
 *
 * @param <S>
 *      the type of scopes
 * @param <L>
 *      the type of labels
 */
public interface IPath<S, L> {

    PSequence<S> scopes();

    Set.Immutable<S> scopeSet();

    PSequence<L> labels();

}