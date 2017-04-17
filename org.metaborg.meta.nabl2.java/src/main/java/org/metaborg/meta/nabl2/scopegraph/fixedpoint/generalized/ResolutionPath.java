package org.metaborg.meta.nabl2.scopegraph.fixedpoint.generalized;

import java.util.Optional;

import org.metaborg.meta.nabl2.scopegraph.ILabel;
import org.metaborg.meta.nabl2.scopegraph.IScope;

public interface ResolutionPath<S extends IScope, L extends ILabel, R, D> extends IPath, Namespaced<R, D> {

    Ref<R, D> getRef();

    ScopePath<S, L> getPath();

    Decl<R, D> getDecl();

    @SuppressWarnings({ "hiding", "unchecked" }) default <R, D> Optional<ResolutionPath<S, L, R, D>>
        cast(Namespace<R, D> ns) {
        if(ns.equals(getNamespace())) {
            return Optional.of((ResolutionPath<S, L, R, D>) this);
        }
        return Optional.empty();
    }

    @SuppressWarnings("hiding") <R, D> Optional<ResolutionPath<S, L, R, D>> match(Namespace<R, D> ns);

}