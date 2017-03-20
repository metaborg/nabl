package org.metaborg.meta.nabl2.scopegraph.fixedpoint.generalized;

import java.util.Optional;

interface Namespaced<R, D> {

    Namespace<R, D> getNamespace();

    @SuppressWarnings("hiding") <R, D> Optional<? extends Namespaced<R, D>> cast(Namespace<R, D> ns);

}