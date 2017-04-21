package org.metaborg.meta.nabl2.scopegraph.fixedpoint.generalized;

import java.util.Set;

import org.metaborg.meta.nabl2.scopegraph.ILabel;
import org.metaborg.meta.nabl2.scopegraph.IScope;

public interface ImportsAndExports<S extends IScope, L extends ILabel> {

    <R, D> Set<Entry<L, Ref<R, D>>> getImports(S scope);

    <R, D> Set<Ref<R, D>> getImports(S scope, L label);

    <R, D> Set<S> getImportScopes(Ref<R, D> ref, L label);

    <R, D> Set<Entry<L, S>> getImportScopes(Ref<R, D> ref);


    <R, D> Set<Entry<L, Decl<R, D>>> getExports(S scope);

    <R, D> Set<Decl<R, D>> getExports(S scope, L label);

    <R, D> Set<S> getExportScopes(Decl<R, D> decl, L label);

    <R, D> Set<Entry<L, S>> getExportScopes(Decl<R, D> decl);


    interface Mutable<S extends IScope, L extends ILabel> extends ImportsAndExports<S, L> {

        <R, D> boolean putExport(S scope, Decl<R, D> decl);

        <R, D> boolean putImport(S scope, Ref<R, D> ref);

    }

    interface Entry<L extends ILabel, V> {

        L getLabel();

        V getValue();

    }

}