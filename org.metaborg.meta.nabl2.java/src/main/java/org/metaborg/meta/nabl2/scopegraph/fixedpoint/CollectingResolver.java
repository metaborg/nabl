package org.metaborg.meta.nabl2.scopegraph.fixedpoint;

import org.metaborg.meta.nabl2.scopegraph.ILabel;
import org.metaborg.meta.nabl2.scopegraph.IOccurrence;
import org.metaborg.meta.nabl2.scopegraph.IScope;
import org.metaborg.meta.nabl2.scopegraph.path.IResolutionPath;
import org.metaborg.meta.nabl2.scopegraph.path.IResolutionPaths;
import org.metaborg.meta.nabl2.scopegraph.path.IScopePath;
import org.metaborg.meta.nabl2.scopegraph.path.IScopePaths;

public class CollectingResolver<S extends IScope, L extends ILabel, O extends IOccurrence>
        implements IPathResolver<S, L, O> {

    private final ScopePaths<S, L, O> scopePaths;
    private final ResolutionPaths<S, L, O> resolutionPaths;

    public CollectingResolver() {
        this.scopePaths = new ScopePaths<>();
        this.resolutionPaths = new ResolutionPaths<>();
    }

    @Override public boolean add(IResolutionPath<S, L, O> path) {
        return resolutionPaths.add(path);
    }

    @Override public boolean add(IScopePath<S, L, O> path) {
        return scopePaths.add(path);
    }

    @Override public boolean shadow(IResolutionPath<S, L, O> path) {
        return resolutionPaths.remove(path);
    }

    @Override public IScopePaths<S, L, O> scopePaths() {
        return scopePaths;
    }

    @Override public IResolutionPaths<S, L, O> resolutionPaths() {
        return resolutionPaths;
    }

}