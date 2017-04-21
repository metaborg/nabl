package org.metaborg.meta.nabl2.scopegraph.fixedpoint;

import java.util.Set;

import org.metaborg.meta.nabl2.scopegraph.ILabel;
import org.metaborg.meta.nabl2.scopegraph.IOccurrence;
import org.metaborg.meta.nabl2.scopegraph.IScope;
import org.metaborg.meta.nabl2.scopegraph.path.IResolutionPath;
import org.metaborg.meta.nabl2.scopegraph.path.IResolutionPaths;
import org.metaborg.meta.nabl2.util.collections.HashIndexedSet;
import org.metaborg.meta.nabl2.util.collections.IIndexedSet;

public class ResolutionPaths<S extends IScope, L extends ILabel, O extends IOccurrence> implements IResolutionPaths<S, L, O> {

    private final IIndexedSet.Mutable<IResolutionPath<S, L, O>, O> fwd;
    private final IIndexedSet.Mutable<IResolutionPath<S, L, O>, O> bwd;

    public ResolutionPaths() {
        this(new HashIndexedSet<>(IResolutionPath::getReference),
                new HashIndexedSet<>(IResolutionPath::getDeclaration));
    }

    private ResolutionPaths(IIndexedSet.Mutable<IResolutionPath<S, L, O>, O> fwd,
            IIndexedSet.Mutable<IResolutionPath<S, L, O>, O> bwd) {
        this.fwd = fwd;
        this.bwd = bwd;
    }

    @Override
    public IResolutionPaths<S, L, O> inverse() {
        return new ResolutionPaths<>(bwd, fwd);
    }

    @Override
    public Set<IResolutionPath<S, L, O>> get(O occurrence) {
        return fwd.get(occurrence);
    }

    public boolean add(IResolutionPath<S, L, O> path) {
        if(fwd.add(path)) {
            bwd.add(path);
            return true;
        }
        return false;
    }

    public boolean remove(IResolutionPath<S, L, O> path) {
        if(fwd.remove(path)) {
            bwd.remove(path);
            return true;
        }
        return false;
    }

}