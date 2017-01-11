package org.metaborg.meta.nabl2.scopegraph;

import java.util.Optional;

import org.metaborg.meta.nabl2.util.functions.PartialFunction0;

import com.google.common.collect.Multimap;

public interface IScopeGraph<S extends IScope, L extends ILabel, O extends IOccurrence> {

    Iterable<S> getAllScopes();

    Iterable<O> getAllDecls();

    Iterable<O> getAllRefs();


    Iterable<O> getDecls(S scope);

    Iterable<O> getRefs(S scope);

    Multimap<L,PartialFunction0<S>> getDirectEdges(S scope);

    Multimap<L,PartialFunction0<O>> getImportRefs(S scope);


    Optional<S> getDeclScope(O decl);

    Multimap<L,S> getAssocScopes(O decl);


    Optional<S> getRefScope(O ref);

}