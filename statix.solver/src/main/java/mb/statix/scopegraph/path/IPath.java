package mb.statix.scopegraph.path;

import io.usethesource.capsule.Set;
import mb.nabl2.util.collections.PSequence;

public interface IPath<S, L> {

    PSequence<S> scopes();

    Set.Immutable<S> scopeSet();

    PSequence<L> labels();

}