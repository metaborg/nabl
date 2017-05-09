package org.metaborg.meta.nabl2.scopegraph;

import java.io.Serializable;

import org.metaborg.meta.nabl2.scopegraph.path.IDeclPath;
import org.metaborg.meta.nabl2.scopegraph.path.IResolutionPath;

import io.usethesource.capsule.Set;

public interface INameResolution<S extends IScope, L extends ILabel, O extends IOccurrence> {

    Set.Immutable<S> getAllScopes();

    Set.Immutable<O> getAllRefs();

    Set.Immutable<IResolutionPath<S, L, O>> resolve(O ref);

    Set.Immutable<IDeclPath<S, L, O>> visible(S scope);

    Set.Immutable<IDeclPath<S, L, O>> reachable(S scope);

    static <S extends IScope, L extends ILabel, O extends IOccurrence> INameResolution<S, L, O> empty() {
        return new EmptyNameResolution<>();
    }

    static class EmptyNameResolution<S extends IScope, L extends ILabel, O extends IOccurrence>
            implements INameResolution<S, L, O>, Serializable {

        private static final long serialVersionUID = 42L;

        @Override public Set.Immutable<S> getAllScopes() {
            return Set.Immutable.of();
        }

        @Override public Set.Immutable<O> getAllRefs() {
            return Set.Immutable.of();
        }

        @Override public Set.Immutable<IResolutionPath<S, L, O>> resolve(O ref) {
            return Set.Immutable.of();
        }

        @Override public Set.Immutable<IDeclPath<S, L, O>> visible(S scope) {
            return Set.Immutable.of();
        }

        @Override public Set.Immutable<IDeclPath<S, L, O>> reachable(S scope) {
            return Set.Immutable.of();
        }

    }

}
