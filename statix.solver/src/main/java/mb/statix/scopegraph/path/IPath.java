package mb.statix.scopegraph.path;

import io.usethesource.capsule.Set;
import mb.nabl2.util.collections.ConsList;

public interface IPath<S, L> {

    ConsList<S> scopes();

    Set.Immutable<S> scopeSet();

    ConsList<L> labels();

}