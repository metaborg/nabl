package org.metaborg.meta.nabl2.relations;

import java.util.List;
import java.util.Optional;

public interface IVariantMatcher<T> {

    Optional<List<Arg<T>>> match(T t);

    T build(Iterable<? extends T> ts);

    interface Arg<T> {

        IVariance getVariance();

        T getValue();

    }

}