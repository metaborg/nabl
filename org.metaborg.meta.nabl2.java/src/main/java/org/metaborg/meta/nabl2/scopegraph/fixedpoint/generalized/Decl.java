package org.metaborg.meta.nabl2.scopegraph.fixedpoint.generalized;

import java.util.Optional;

interface Decl<R, D> extends Namespaced<R, D> {

    D getDecl();

    @SuppressWarnings({ "hiding", "unchecked" }) @Override default <R, D> Optional<Decl<R, D>>
        cast(Namespace<R, D> ns) {
        if(ns.equals(getNamespace())) {
            return Optional.of((Decl<R, D>) this);
        }
        return Optional.empty();
    }

    static <R, D> Decl<R, D> of(Namespace<R, D> ns, D decl) {
        return null;
    }

}