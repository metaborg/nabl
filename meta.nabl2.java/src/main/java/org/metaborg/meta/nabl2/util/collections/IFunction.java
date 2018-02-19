package org.metaborg.meta.nabl2.util.collections;

import java.util.Map;
import java.util.Optional;

import org.metaborg.meta.nabl2.util.ImmutableTuple2;
import org.metaborg.meta.nabl2.util.Tuple2;
import org.metaborg.util.functions.Function2;

import com.google.common.annotations.Beta;

public interface IFunction<K, V> {

    IInverseFunction<V, K> inverse();

    boolean containsKey(K key);

    boolean containsEntry(K key, V value);

    boolean containsValue(V value);

    java.util.Set<K> keySet();

    java.util.Set<Map.Entry<K, V>> entrySet();

    java.util.Set<V> valueSet();

    Optional<V> get(K key);

    @Beta default java.util.stream.Stream<Tuple2<K, V>> stream() {
        return this.stream(ImmutableTuple2::of);
    }

    @Beta default <R> java.util.stream.Stream<R> stream(final Function2<K, V, R> converter) {
        return this.keySet().stream().map(key -> converter.apply(key, this.get(key).get()));
    }

    interface Immutable<K, V> extends IFunction<K, V> {

        IInverseFunction.Immutable<V, K> inverse();

        Transient<K, V> melt();

    }

    interface Transient<K, V> extends IFunction<K, V> {

        boolean put(K key, V value);

        boolean putAll(IFunction<K, V> other);

        boolean remove(K key);

        IInverseFunction.Transient<V, K> inverse();

        Immutable<K, V> freeze();

    }

}