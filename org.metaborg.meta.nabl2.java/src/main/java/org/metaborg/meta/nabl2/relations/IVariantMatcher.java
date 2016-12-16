package org.metaborg.meta.nabl2.relations;

import java.util.Optional;

public interface IVariantMatcher<T> {

    Optional<Iterable<Arg<T>>> match(T t);

    T build(Iterable<? extends T> ts);

    interface Arg<T> {

        IVariance getVariance();

        T getValue();

    }

}