package org.metaborg.meta.nabl2.scopegraph;

import org.metaborg.meta.nabl2.util.collections.IFunction;
import org.metaborg.meta.nabl2.util.collections.IRelation3;

import io.usethesource.capsule.Set;

public interface IScopeGraph<S extends IScope, L extends ILabel, O extends IOccurrence> {

    Set.Immutable<S> getAllScopes();

    Set.Immutable<O> getAllDecls();

    Set.Immutable<O> getAllRefs();

    IFunction<O, S> getDecls();

    IFunction<O, S> getRefs();

    IRelation3<S, L, S> getDirectEdges();

    IRelation3<O, L, S> getExportEdges();

    IRelation3<S, L, O> getImportEdges();

}