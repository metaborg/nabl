package org.metaborg.meta.nabl2.scopegraph.fixedpoint;

import org.metaborg.meta.nabl2.scopegraph.ILabel;
import org.metaborg.meta.nabl2.scopegraph.IOccurrence;
import org.metaborg.meta.nabl2.scopegraph.IScope;
import org.metaborg.meta.nabl2.scopegraph.path.IResolutionPath;
import org.metaborg.meta.nabl2.scopegraph.path.IResolutionPaths;
import org.metaborg.meta.nabl2.scopegraph.path.IScopePath;
import org.metaborg.meta.nabl2.scopegraph.path.IScopePaths;

public interface IPathResolver<S extends IScope, L extends ILabel, O extends IOccurrence> {

    boolean add(IScopePath<S, L, O> path);

    boolean add(IResolutionPath<S, L, O> path);

    boolean shadow(IResolutionPath<S, L, O> path);

    IScopePaths<S, L, O> scopePaths();

    IResolutionPaths<S, L, O> resolutionPaths();

}