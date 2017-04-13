package org.metaborg.meta.nabl2.util.collections;

import java.util.Set;

public interface IRelation2<K, V> {

    IRelation2<V, K> inverse();

    boolean containsKey(K key);

    boolean containsEntry(K key, V value);

    boolean containsValue(V value);

    Set<K> keySet();

    Set<V> valueSet();

    Set<V> get(K key);

    interface Mutable<K, V> extends IRelation2<K, V> {

        boolean put(K key, V value);

        boolean remove(K key, V value);

    }

}