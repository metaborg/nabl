package mb.statix.scopegraph.path;

import io.usethesource.capsule.Set;
import mb.nabl2.util.collections.PSequence;

public interface IPath<V, L> {

    PSequence<V> scopes();

    Set.Immutable<V> scopeSet();

    PSequence<L> labels();

}