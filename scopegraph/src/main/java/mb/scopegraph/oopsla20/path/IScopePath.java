package mb.scopegraph.oopsla20.path;

import org.metaborg.util.collection.ImmutableCollection;

public interface IScopePath<S, L> extends IPath<S, L>, ImmutableCollection<IStep<S, L>> {

    S getSource();

    S getTarget();

    String toString(boolean includeSource, boolean includeTarget);

}