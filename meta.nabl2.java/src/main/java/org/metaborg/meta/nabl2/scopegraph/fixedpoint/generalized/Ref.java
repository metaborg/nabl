package org.metaborg.meta.nabl2.scopegraph.fixedpoint.generalized;

import java.util.Optional;

interface Ref<R, D> extends Namespaced<R, D> {

    R getRef();

    @SuppressWarnings({ "hiding", "unchecked" }) @Override default <R, D> Optional<Ref<R, D>> cast(Namespace<R, D> ns) {
        if(ns.equals(getNamespace())) {
            return Optional.of((Ref<R, D>) this);
        }
        return Optional.empty();
    }

    static <R, D> Ref<R, D> of(Namespace<R, D> ns, R ref) {
        return null;
    }

}