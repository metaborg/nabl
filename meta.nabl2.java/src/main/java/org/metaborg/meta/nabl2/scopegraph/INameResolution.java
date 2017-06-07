package org.metaborg.meta.nabl2.scopegraph;

import java.util.Optional;

import org.metaborg.meta.nabl2.scopegraph.path.IResolutionPath;

import io.usethesource.capsule.Set;

public interface INameResolution<S extends IScope, L extends ILabel, O extends IOccurrence> {

    Set<S> getAllScopes();

    Set<O> getAllRefs();

    Set.Immutable<IResolutionPath<S, L, O>> resolve(O ref);

    Set.Immutable<O> visible(S scope);

    Set.Immutable<O> reachable(S scope);

    interface Immutable<S extends IScope, L extends ILabel, O extends IOccurrence> extends INameResolution<S, L, O> {

    }

    interface Transient<S extends IScope, L extends ILabel, O extends IOccurrence> extends INameResolution<S, L, O> {

        Optional<Set.Immutable<IResolutionPath<S, L, O>>> tryResolve(O ref);

        Optional<Set.Immutable<O>> tryVisible(S scope);

        Optional<Set.Immutable<O>> tryReachable(S scope);

    }

}