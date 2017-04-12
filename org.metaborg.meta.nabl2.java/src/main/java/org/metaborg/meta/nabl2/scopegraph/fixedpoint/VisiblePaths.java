package org.metaborg.meta.nabl2.scopegraph.fixedpoint;

import org.metaborg.meta.nabl2.regexp.IRegExpMatcher;
import org.metaborg.meta.nabl2.regexp.RegExpMatcher;
import org.metaborg.meta.nabl2.scopegraph.ILabel;
import org.metaborg.meta.nabl2.scopegraph.IOccurrence;
import org.metaborg.meta.nabl2.scopegraph.IResolutionParameters;
import org.metaborg.meta.nabl2.scopegraph.IScope;
import org.metaborg.meta.nabl2.scopegraph.path.IResolutionPath;
import org.metaborg.meta.nabl2.scopegraph.path.IScopePath;
import org.metaborg.meta.nabl2.util.collections.HashRelation3;
import org.metaborg.meta.nabl2.util.collections.IRelation3;

public class VisiblePaths<S extends IScope, L extends ILabel, O extends IOccurrence> implements IPathObserver<S, L, O> {

    private final IRegExpMatcher<L> wf;
    private final IRelation3.Mutable<S, IScopePath<S, L, O>, S> visibility;
    private final IRelation3.Mutable<O, IResolutionPath<S, L, O>, O> resolution;

    public VisiblePaths(IResolutionParameters<L> params) {
        this.wf = RegExpMatcher.create(params.getPathWf());
        this.visibility = HashRelation3.create();
        this.resolution = HashRelation3.create();
    }

    @Override public void add(IResolutionPath<S, L, O> path) {
        if(wf.match(path.getLabels()).isAccepting()) {
            resolution.put(path.getReference(), path, path.getDeclaration());
        }
    }

    @Override public void add(IScopePath<S, L, O> path) {
        if(wf.match(path.getLabels()).isAccepting()) {
            visibility.put(path.getSource(), path, path.getTarget());
        }
    }

    @Override public IRelation3<S, IScopePath<S, L, O>, S> scopePaths() {
        return visibility;
    }

    @Override public IRelation3<O, IResolutionPath<S, L, O>, O> resolutionPaths() {
        return resolution;
    }

}