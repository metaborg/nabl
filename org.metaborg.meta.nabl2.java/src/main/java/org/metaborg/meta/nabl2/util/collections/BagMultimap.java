package org.metaborg.meta.nabl2.util.collections;

import com.google.common.collect.Multiset;

public interface BagMultimap<K, V> {

    void put(K key, V value);

    void remove(K key, V value);

    boolean containsKey(K key);

    boolean containsEntry(K key, V value);

    Multiset<V> get(K key);

}