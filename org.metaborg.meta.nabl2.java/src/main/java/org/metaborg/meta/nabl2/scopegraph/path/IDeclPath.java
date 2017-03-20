package org.metaborg.meta.nabl2.scopegraph.path;

import org.metaborg.meta.nabl2.scopegraph.ILabel;
import org.metaborg.meta.nabl2.scopegraph.IOccurrence;
import org.metaborg.meta.nabl2.scopegraph.IScope;

public interface IDeclPath<S extends IScope, L extends ILabel, O extends IOccurrence> extends IPath<S, L, O> {

    IScopePath<S, L, O> getPath();

    O getDeclaration();

}