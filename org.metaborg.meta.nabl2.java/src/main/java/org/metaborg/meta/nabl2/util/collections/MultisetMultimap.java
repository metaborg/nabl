package org.metaborg.meta.nabl2.util.collections;

import java.util.Collection;
import java.util.Set;

import com.google.common.collect.Multiset;

public interface MultisetMultimap<K, V> {

    Set<K> keySet();
    
    void put(K key, V value);

    void putAll(K key, Collection<V> values);

    boolean remove(K key, V value);

    boolean removeAll(K key);

    boolean removeAll(K key, Collection<V> values);

    boolean containsKey(K key);

    boolean containsEntry(K key, V value);

    Multiset<V> get(K key);

}