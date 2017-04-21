package org.metaborg.meta.nabl2.scopegraph.fixedpoint;

import org.metaborg.meta.nabl2.regexp.IRegExpMatcher;
import org.metaborg.meta.nabl2.regexp.RegExpMatcher;
import org.metaborg.meta.nabl2.scopegraph.ILabel;
import org.metaborg.meta.nabl2.scopegraph.IOccurrence;
import org.metaborg.meta.nabl2.scopegraph.IResolutionParameters;
import org.metaborg.meta.nabl2.scopegraph.IScope;
import org.metaborg.meta.nabl2.scopegraph.path.IResolutionPath;
import org.metaborg.meta.nabl2.scopegraph.path.IResolutionPaths;
import org.metaborg.meta.nabl2.scopegraph.path.IScopePath;
import org.metaborg.meta.nabl2.scopegraph.path.IScopePaths;

public class WellFormedResolver<S extends IScope, L extends ILabel, O extends IOccurrence>
        implements IPathResolver<S, L, O> {

    private final IRegExpMatcher<L> wf;
    private final IPathResolver<S, L, O> next;

    public WellFormedResolver(IResolutionParameters<L> params, IPathResolver<S, L, O> next) {
        this.wf = RegExpMatcher.create(params.getPathWf());
        this.next = next;
    }

    @Override public boolean add(IResolutionPath<S, L, O> path) {
        if(wf.match(path.getLabels()).isAccepting()) {
            return next.add(path);
        }
        return false;
    }

    @Override public boolean add(IScopePath<S, L, O> path) {
        if(wf.match(path.getLabels()).isAccepting()) {
            return next.add(path);
        }
        return false;
    }

    @Override public boolean shadow(IResolutionPath<S, L, O> path) {
        return next.shadow(path);
    }

    @Override public IScopePaths<S, L, O> scopePaths() {
        return next.scopePaths();
    }

    @Override public IResolutionPaths<S, L, O> resolutionPaths() {
        return next.resolutionPaths();
    }

}