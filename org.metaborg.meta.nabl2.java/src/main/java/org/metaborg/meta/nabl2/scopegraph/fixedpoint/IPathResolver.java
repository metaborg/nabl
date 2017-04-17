package org.metaborg.meta.nabl2.scopegraph.fixedpoint;

import org.metaborg.meta.nabl2.scopegraph.ILabel;
import org.metaborg.meta.nabl2.scopegraph.IOccurrence;
import org.metaborg.meta.nabl2.scopegraph.IScope;
import org.metaborg.meta.nabl2.scopegraph.path.IResolutionPath;
import org.metaborg.meta.nabl2.scopegraph.path.IScopePath;
import org.metaborg.meta.nabl2.util.collections.IRelation3;

public interface IPathResolver<S extends IScope, L extends ILabel, O extends IOccurrence> {

    boolean add(IResolutionPath<S, L, O> path);

    boolean add(IScopePath<S, L, O> path);

    IRelation3<S, IScopePath<S, L, O>, S> scopePaths();

    IRelation3<O, IResolutionPath<S, L, O>, O> resolutionPaths();

}