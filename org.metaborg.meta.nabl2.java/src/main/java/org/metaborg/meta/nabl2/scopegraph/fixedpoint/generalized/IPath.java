package org.metaborg.meta.nabl2.scopegraph.fixedpoint.generalized;

import org.metaborg.meta.nabl2.scopegraph.ILabel;
import org.metaborg.meta.nabl2.scopegraph.IScope;

public interface IPath {

    static <S extends IScope, L extends ILabel> ScopePath<S, L> of(ScopePath<S, L> pre, IStep<S, L> step)
        throws PathException {
        return null;
    }

    static <S extends IScope, L extends ILabel> ScopePath<S, L> of(IStep<S, L> step, ScopePath<S, L> post)
        throws PathException {
        return null;
    }

    static <S extends IScope, L extends ILabel> ScopePath<S, L> of(IStep<S, L> step) throws PathException {
        return null;
    }

    static <S extends IScope, L extends ILabel> ScopePath<S, L> of(ScopePath<S, L> pre, IStep<S, L> step,
        ScopePath<S, L> post) throws PathException {
        return null;
    }


    static <S extends IScope, L extends ILabel, R, D> ResolutionPath<S, L, R, D> of(Ref<R, D> ref, ScopePath<S, L> path,
        Decl<R, D> decl) throws PathException {
        if(!ref.getNamespace().match(ref.getRef(), decl.getDecl())) {
            throw new PathException();
        }
        return null;
    }

}