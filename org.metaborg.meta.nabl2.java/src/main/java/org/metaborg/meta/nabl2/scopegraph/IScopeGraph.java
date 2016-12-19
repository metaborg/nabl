package org.metaborg.meta.nabl2.scopegraph;

import org.metaborg.meta.nabl2.util.functions.PartialFunction0;

public interface IScopeGraph<S extends IScope, L extends ILabel, O extends IOccurrence> {

    Iterable<S> getAllScopes();

    Iterable<O> getAllDecls();

    Iterable<O> getAllRefs();

    Iterable<O> getDecls(S scope);

    Iterable<O> getRefs(S scope);

    Iterable<PartialFunction0<S>> getDirectEdges(S scope, L label);

    Iterable<S> getAssocs(O decl, L label);

    Iterable<PartialFunction0<O>> getImports(S scope, L label);

}