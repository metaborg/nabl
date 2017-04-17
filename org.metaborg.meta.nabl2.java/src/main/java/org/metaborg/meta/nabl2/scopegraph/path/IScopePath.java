package org.metaborg.meta.nabl2.scopegraph.path;

import org.metaborg.meta.nabl2.scopegraph.ILabel;
import org.metaborg.meta.nabl2.scopegraph.IOccurrence;
import org.metaborg.meta.nabl2.scopegraph.IScope;

public interface IScopePath<S extends IScope, L extends ILabel, O extends IOccurrence>
        extends IPath<S, L, O>, Iterable<IStep<S, L, O>> {

    S getSource();

    S getTarget();

    int size();

    String toString(boolean includeSource, boolean includeTarget);

}