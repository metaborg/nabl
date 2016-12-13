package org.metaborg.meta.nabl2.scopegraph;

import org.metaborg.util.iterators.Iterables2;

public interface INameResolution<S extends IScope, L extends ILabel, O extends IOccurrence> {

    Iterable<O> resolve(O ref);

    Iterable<O> visible(S scope);

    Iterable<O> reachable(S scope);

    static <S extends IScope, L extends ILabel, O extends IOccurrence> INameResolution<S,L,O> empty() {
        return new INameResolution<S,L,O>() {

            @Override public Iterable<O> resolve(O ref) {
                return Iterables2.empty();
            }

            @Override public Iterable<O> visible(S scope) {
                return Iterables2.empty();
            }

            @Override public Iterable<O> reachable(S scope) {
                return Iterables2.empty();
            }

        };
    }

}