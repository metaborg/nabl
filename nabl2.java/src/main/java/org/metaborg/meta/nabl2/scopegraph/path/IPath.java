package org.metaborg.meta.nabl2.scopegraph.path;

import org.metaborg.meta.nabl2.scopegraph.ILabel;
import org.metaborg.meta.nabl2.scopegraph.IOccurrence;
import org.metaborg.meta.nabl2.scopegraph.IScope;
import org.metaborg.meta.nabl2.util.collections.PSequence;

import io.usethesource.capsule.Set;

public interface IPath<S extends IScope, L extends ILabel, O extends IOccurrence> {

    Set.Immutable<O> getImports();

    Set.Immutable<S> getScopes();

    PSequence<L> getLabels();

    Iterable<IResolutionPath<S, L, O>> getImportPaths();

}