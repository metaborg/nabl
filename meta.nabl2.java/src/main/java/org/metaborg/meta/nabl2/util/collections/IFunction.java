package org.metaborg.meta.nabl2.util.collections;

import java.util.Optional;
import java.util.Set;

public interface IFunction<K, V> {

    IInverseFunction<V, K> inverse();

    boolean containsKey(K key);

    boolean containsEntry(K key, V value);

    boolean containsValue(V value);

    Set<K> keySet();

    Set<V> valueSet();

    Optional<V> get(K key);

    interface Mutable<K, V> extends IFunction<K, V> {

        boolean put(K key, V value);

        boolean remove(K key);

    }

}