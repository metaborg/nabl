package org.metaborg.meta.nabl2.scopegraph;

public interface IScopeGraph<S extends IScope, L extends ILabel, O extends IOccurrence> {

    Iterable<S> getAllScopes();

    Iterable<O> getAllDecls();

    Iterable<O> getAllRefs();

    Iterable<O> getDecls(S scope);

    Iterable<O> getRefs(S scope);

    Iterable<S> getDirectEdges(S scope, L label);

    Iterable<S> getAssocs(O decl, L label);

    Iterable<O> getImports(S scope, L label);

}