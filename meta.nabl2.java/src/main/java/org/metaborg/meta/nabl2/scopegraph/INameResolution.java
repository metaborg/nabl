package org.metaborg.meta.nabl2.scopegraph;

import java.util.Map;
import java.util.Optional;

import org.metaborg.meta.nabl2.scopegraph.path.IResolutionPath;

import io.usethesource.capsule.Set;

public interface INameResolution<S extends IScope, L extends ILabel, O extends IOccurrence> {

    java.util.Set<O> getResolvedRefs();

    Optional<Set.Immutable<IResolutionPath<S, L, O>>> resolve(O ref);

    Optional<Set.Immutable<O>> visible(S scope);

    Optional<Set.Immutable<O>> reachable(S scope);

    java.util.Set<Map.Entry<O, Set.Immutable<IResolutionPath<S, L, O>>>> resolutionEntries();

    interface Immutable<S extends IScope, L extends ILabel, O extends IOccurrence> extends INameResolution<S, L, O> {

    }

    interface Transient<S extends IScope, L extends ILabel, O extends IOccurrence> extends INameResolution<S, L, O> {

    }

}