package org.metaborg.meta.nabl2.scopegraph;

import org.metaborg.util.iterators.Iterables2;

public interface INameResolution<S extends IScope, L extends ILabel, O extends IOccurrence> {

    Iterable<IPath<S,L,O>> resolve(O ref);

    Iterable<IPath<S,L,O>> visible(S scope);

    Iterable<IPath<S,L,O>> reachable(S scope);

    static <S extends IScope, L extends ILabel, O extends IOccurrence> INameResolution<S,L,O> empty() {
        return new INameResolution<S,L,O>() {

            @Override public Iterable<IPath<S,L,O>> resolve(O ref) {
                return Iterables2.empty();
            }

            @Override public Iterable<IPath<S,L,O>> visible(S scope) {
                return Iterables2.empty();
            }

            @Override public Iterable<IPath<S,L,O>> reachable(S scope) {
                return Iterables2.empty();
            }

        };
    }

}