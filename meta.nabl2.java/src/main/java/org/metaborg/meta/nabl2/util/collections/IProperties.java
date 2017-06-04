package org.metaborg.meta.nabl2.util.collections;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import org.metaborg.meta.nabl2.util.tuples.Tuple3;

public interface IProperties<I, K, V> {

    Set<I> getIndices();

    Set<K> getDefinedKeys(I index);

    Optional<V> getValue(I index, K key);

    public Stream<Tuple3<I, K, V>> stream();

    interface Immutable<I, K, V> extends IProperties<I, K, V> {

        IProperties.Transient<I, K, V> melt();

    }

    interface Transient<I, K, V> extends IProperties<I, K, V> {

        Optional<V> putValue(I index, K key, V value);

        IProperties.Immutable<I, K, V> freeze();

    }

}