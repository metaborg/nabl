package org.metaborg.meta.nabl2.scopegraph;

import org.metaborg.meta.nabl2.terms.ITermIndex;

public interface IScopeGraph<S extends IScope, L extends ILabel, O extends IOccurrence> {

    Iterable<S> getAllScopes();

    Iterable<O> getDecls(S scope);

    Iterable<O> getRefs(S scope);

    Iterable<S> getDirectEdges(S scope, L label);

    Iterable<S> getAssocs(O decl, L label);

    Iterable<O> getImports(S scope, L label);

    Iterable<O> getAstRefs(ITermIndex index);
    
}