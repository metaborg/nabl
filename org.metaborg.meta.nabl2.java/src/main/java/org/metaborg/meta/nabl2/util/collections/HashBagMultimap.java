package org.metaborg.meta.nabl2.util.collections;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;
import com.google.common.collect.Multisets;

public class HashBagMultimap<K, V> implements BagMultimap<K, V>, Serializable {

    private static final long serialVersionUID = 42L;

    private final Map<K, Multiset<V>> data;

    private HashBagMultimap(Map<K, Multiset<V>> data) {
        this.data = data;
    }

    @Override public Set<K> keySet() {
        return data.keySet();
    }

    @Override public void put(K key, V value) {
        data.computeIfAbsent(key, k -> HashMultiset.create()).add(value);
    }

    @Override public void putAll(K key, Collection<V> values) {
        data.computeIfAbsent(key, k -> HashMultiset.create()).addAll(values);
    }

    @Override public boolean remove(K key, V value) {
        if(data.containsKey(key)) {
            return data.get(key).remove(value);
        }
        return false;
    }

    @Override public boolean removeAll(K key) {
        Multiset<V> elems = data.remove(key);
        return elems != null && !elems.isEmpty();
    }

    @Override public boolean removeAll(K key, Collection<V> values) {
        if(data.containsKey(key)) {
            return data.get(key).removeAll(values);
        }
        return false;
    }

    @Override public boolean containsKey(K key) {
        return data.containsKey(key) && !data.get(key).isEmpty();
    }

    @Override public boolean containsEntry(K key, V value) {
        return data.containsKey(key) && data.get(key).contains(value);
    }

    @Override public Multiset<V> get(K key) {
        return Multisets.unmodifiableMultiset(data.containsKey(key) ? data.get(key) : HashMultiset.create());
    }

    public static <K, V> BagMultimap<K, V> create() {
        return new HashBagMultimap<>(new HashMap<>());
    }

}
