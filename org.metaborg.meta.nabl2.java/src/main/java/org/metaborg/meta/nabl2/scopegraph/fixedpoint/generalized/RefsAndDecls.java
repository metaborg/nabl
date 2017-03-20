package org.metaborg.meta.nabl2.scopegraph.fixedpoint.generalized;

import java.util.Set;

import org.metaborg.meta.nabl2.scopegraph.ILabel;
import org.metaborg.meta.nabl2.scopegraph.IScope;

public interface RefsAndDecls<S extends IScope, L extends ILabel> {

    <R, D> S getScope(Ref<R, D> ref);

    <R, D> S getScope(Decl<R, D> decl);

    Set<Ref<?, ?>> getRefs(S scope);

    Set<Decl<?, ?>> getDecls(S scope);

    <R, D> Set<Ref<R, D>> getRefs(S scope, Namespace<R, D> ns);

    <R, D> Set<Decl<R, D>> getDecls(S scope, Namespace<R, D> ns);

    interface Mutable<S extends IScope, L extends ILabel> extends RefsAndDecls<S, L> {

        <R, D> boolean putDecl(S scope, Decl<R, D> decl);

        <R, D> boolean putRef(S scope, Ref<R, D> ref);

    }

}