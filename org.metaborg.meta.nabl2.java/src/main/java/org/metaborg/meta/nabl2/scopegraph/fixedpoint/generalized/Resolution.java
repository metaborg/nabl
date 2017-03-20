package org.metaborg.meta.nabl2.scopegraph.fixedpoint.generalized;

import java.util.Set;

import org.metaborg.meta.nabl2.scopegraph.ILabel;
import org.metaborg.meta.nabl2.scopegraph.IScope;

public interface Resolution<S extends IScope, L extends ILabel> {

    Set<ResolutionPath<S, L, ?, ?>> all();

  //<R, D> Set<ResolutionPath<S, L, R, D>> get(Namespace<R, D> ns);

    <R, D> Set<ResolutionPath<S, L, R, D>> from(Ref<R, D> ref);

    <R, D> Set<ResolutionPath<S, L, R, D>> to(Decl<R, D> decl);

    interface Mutable<S extends IScope, L extends ILabel> extends Resolution<S, L> {

        <R, D> boolean add(ResolutionPath<S, L, R, D> path);

    }

}