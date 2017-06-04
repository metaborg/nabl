package org.metaborg.meta.nabl2.util.collections;

import java.util.Map;

import org.metaborg.meta.nabl2.util.functions.Function3;
import org.metaborg.meta.nabl2.util.tuples.ImmutableTuple3;
import org.metaborg.meta.nabl2.util.tuples.Tuple3;

import com.google.common.annotations.Beta;

public interface IRelation3<K, L, V> {

    IRelation3<V, L, K> inverse();

    java.util.Set<K> keySet();

    java.util.Set<V> valueSet();

    boolean contains(K key);

    boolean contains(K key, L label);

    boolean contains(K key, L label, V value);

    boolean isEmpty();

    java.util.Set<? extends Map.Entry<L, V>> get(K key);

    java.util.Set<V> get(K key, L label);

    @Beta default java.util.stream.Stream<Tuple3<K, L, V>> stream() {
        return this.stream(ImmutableTuple3::of);
    }

    @Beta default <R> java.util.stream.Stream<R> stream(final Function3<K, L, V, R> converter) {
        return this.keySet().stream().flatMap(
                key -> this.get(key).stream().map(entry -> converter.apply(key, entry.getKey(), entry.getValue())));
    }

    interface Immutable<K, L, V> extends IRelation3<K, L, V> {

        IRelation3.Transient<K, L, V> melt();

    }

    interface Transient<K, L, V> extends IRelation3<K, L, V> {

        boolean put(K key, L label, V value);

        boolean putAll(IRelation3<K, L, V> other);

        boolean remove(K key);

        boolean remove(K key, L label);

        boolean remove(K key, L label, V value);

        IRelation3.Immutable<K, L, V> freeze();

    }

}