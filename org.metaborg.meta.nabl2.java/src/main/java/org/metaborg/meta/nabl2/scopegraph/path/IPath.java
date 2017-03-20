package org.metaborg.meta.nabl2.scopegraph.path;

import org.metaborg.meta.nabl2.scopegraph.ILabel;
import org.metaborg.meta.nabl2.scopegraph.IOccurrence;
import org.metaborg.meta.nabl2.scopegraph.IScope;
import org.pcollections.PSequence;
import org.pcollections.PSet;

public interface IPath<S extends IScope, L extends ILabel, O extends IOccurrence> {

    PSet<O> getImports();

    PSet<S> getScopes();

    PSequence<L> getLabels();

    Iterable<IResolutionPath<S, L, O>> getImportPaths();

}