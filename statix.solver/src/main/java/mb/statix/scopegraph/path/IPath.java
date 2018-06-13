package mb.statix.scopegraph.path;

import io.usethesource.capsule.Set;
import mb.nabl2.util.collections.PSequence;

public interface IPath<V, L> {

    Set.Immutable<V> getScopes();

    PSequence<L> getLabels();

}