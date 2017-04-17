package org.metaborg.meta.nabl2.util.collections;

import java.util.Map;

public interface IRelation3<K, L, V> {

    IRelation3<V, L, K> inverse();

    ISet<K> keySet();

    ISet<V> valueSet();

    boolean contains(K key);

    boolean contains(K key, L label);

    boolean contains(K key, L label, V value);

    ISet<Map.Entry<L, V>> get(K key);

    ISet<V> get(K key, L label);

    interface Mutable<K, L, V> extends IRelation3<K, L, V> {

        boolean put(K key, L label, V value);

        boolean remove(K key);

        boolean remove(K key, L label);

        boolean remove(K key, L label, V value);

    }

}