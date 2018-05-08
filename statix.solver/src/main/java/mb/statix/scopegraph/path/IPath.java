package mb.statix.scopegraph.path;

import io.usethesource.capsule.Set;
import mb.nabl2.util.collections.PSequence;

public interface IPath<S, L, O> {

    Set.Immutable<S> getScopes();

    PSequence<L> getLabels();

}