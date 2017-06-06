package org.metaborg.meta.nabl2.util.collections;

import java.util.Map;

import org.metaborg.meta.nabl2.util.tuples.Tuple2;

import com.google.common.annotations.Beta;

public interface IRelation2<K, V> {

    IRelation2<V, K> inverse();

    boolean containsKey(K key);

    boolean containsEntry(K key, V value);

    boolean containsValue(V value);

    boolean isEmpty();

    java.util.Set<V> get(K key);

    java.util.Set<K> keySet();

    java.util.Set<V> valueSet();

    java.util.Set<Map.Entry<K, V>> entrySet();

    @Beta default java.util.stream.Stream<Tuple2<K, V>> stream() {
        return this.entrySet().stream().map(Tuple2::of);
    }

    interface Immutable<K, V> extends IRelation2<K, V> {

        IRelation2.Transient<K, V> melt();

    }

    interface Transient<K, V> extends IRelation2<K, V> {

        boolean put(K key, V value);

        boolean putAll(IRelation2<K, V> other);

        boolean removeKey(K key);

        boolean removeValue(V value);

        boolean removeEntry(K key, V value);

        IRelation2.Immutable<K, V> freeze();

    }

}