package org.metaborg.meta.nabl2.scopegraph.fixedpoint;

import java.util.Set;

import org.metaborg.meta.nabl2.scopegraph.ILabel;
import org.metaborg.meta.nabl2.scopegraph.IOccurrence;
import org.metaborg.meta.nabl2.scopegraph.IScope;
import org.metaborg.meta.nabl2.scopegraph.path.IScopePath;
import org.metaborg.meta.nabl2.scopegraph.path.IScopePaths;
import org.metaborg.meta.nabl2.util.collections.HashIndexedSet;
import org.metaborg.meta.nabl2.util.collections.IIndexedSet;

public class ScopePaths<S extends IScope, L extends ILabel, O extends IOccurrence> implements IScopePaths<S, L, O> {

    private final IIndexedSet.Mutable<IScopePath<S, L, O>, S> fwd;
    private final IIndexedSet.Mutable<IScopePath<S, L, O>, S> bwd;

    public ScopePaths() {
        this(new HashIndexedSet<>(IScopePath::getSource), new HashIndexedSet<>(IScopePath::getTarget));
    }

    private ScopePaths(IIndexedSet.Mutable<IScopePath<S, L, O>, S> fwd,
            IIndexedSet.Mutable<IScopePath<S, L, O>, S> bwd) {
        this.fwd = fwd;
        this.bwd = bwd;
    }

    @Override public IScopePaths<S, L, O> inverse() {
        return new ScopePaths<>(bwd, fwd);
    }

    @Override public Set<IScopePath<S, L, O>> get(S scope) {
        return fwd.get(scope);
    }

    public boolean add(IScopePath<S, L, O> path) {
        if(fwd.add(path)) {
            bwd.add(path);
            return true;
        }
        return false;
    }

    public boolean remove(IScopePath<S, L, O> path) {
        if(fwd.remove(path)) {
            bwd.remove(path);
            return true;
        }
        return false;
    }

}