package org.metaborg.meta.nabl2.scopegraph;

import org.metaborg.meta.nabl2.util.collections.IFunction;
import org.metaborg.meta.nabl2.util.collections.IRelation3;
import org.metaborg.meta.nabl2.util.collections.ISet;

public interface IScopeGraph<S extends IScope, L extends ILabel, O extends IOccurrence> {

    ISet<S> getAllScopes();

    ISet<O> getAllDecls();

    ISet<O> getAllRefs();


    IFunction<O, S> getDecls();

    IFunction<O, S> getRefs();

    IRelation3<S, L, S> getDirectEdges();

    IRelation3<O, L, S> getExportEdges();

    IRelation3<S, L, O> getImportEdges();

}