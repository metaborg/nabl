package mb.scopegraph.oopsla20.path;

import org.metaborg.util.collection.ConsList;

import io.usethesource.capsule.Set;

public interface IPath<S, L> {

    ConsList<S> scopes();

    Set.Immutable<S> scopeSet();

    ConsList<L> labels();

}