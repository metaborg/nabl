package org.metaborg.meta.nabl2.util.fastutil;

import java.util.Set;

public interface Object2ObjectPMap<K, V> {

    boolean containsKey(K key);

    V get(K key);

    Object2ObjectPMap<K,V> put(K key, V value);

    Object2ObjectPMap<K,V> remove(K key);

    Set<K> keySet();

}