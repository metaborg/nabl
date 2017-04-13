package org.metaborg.meta.nabl2.util.collections;

import java.util.Set;

public interface IInverseFunction<K, V> {

    IFunction<V, K> inverse();

    boolean containsKey(K key);

    boolean containsEntry(K key, V value);

    boolean containsValue(V value);

    Set<K> keySet();

    Set<V> valueSet();

    Set<V> get(K key);

    interface Mutable<K, V> extends IInverseFunction<K, V> {

        boolean put(K key, V value);

        boolean remove(K key, V value);

    }

}