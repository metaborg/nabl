package org.metaborg.meta.nabl2.collections.pcollections;

import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.pcollections.HashTreePMap;
import org.pcollections.HashTreePSet;
import org.pcollections.PMap;
import org.pcollections.PSet;

import com.google.common.collect.Multimap;
import com.google.common.collect.Multiset;

public class HashTreePMultimap<K, V> implements PMultimap<K,V> {

    private final PMap<K,PSet<V>> data;

    public HashTreePMultimap() {
        this.data = HashTreePMap.empty();
    }

    private HashTreePMultimap(PMap<K,PSet<V>> data) {
        this.data = data;
    }

    @Override public PMultimap<K,V> plus(K key, V value) {
        final PSet<V> newValues = get(key).plus(value);
        final PMap<K,PSet<V>> newData = data.plus(key, newValues);
        return new HashTreePMultimap<>(newData);
    }

    @Override public PMultimap<K,V> plusAll(K key, Collection<? extends V> values) {
        final PSet<V> newValues = get(key).plusAll(values);
        final PMap<K,PSet<V>> newData = data.plus(key, newValues);
        return new HashTreePMultimap<>(newData);
    }

    @Override public PMultimap<K,V> plusAll(Multimap<? extends K,? extends V> multimap) {
        PMap<K,PSet<V>> newData = data;
        for (Entry<? extends K,? extends V> entry : multimap.entries()) {
            final K key = entry.getKey();
            final PSet<V> newValues = get(key).plus(entry.getValue());
            newData = newData.plus(key, newValues);
        }
        return new HashTreePMultimap<>(newData);
    }

    @Override public PMultimap<K,V> minus(K key) {
        final PMap<K,PSet<V>> newData = data.minus(key);
        return new HashTreePMultimap<>(newData);
    }

    @Override public PMultimap<K,V> minus(K key, V value) {
        final PSet<V> newValues = get(key).minus(value);
        final PMap<K,PSet<V>> newData = data.plus(key, newValues);
        return new HashTreePMultimap<>(newData);
    }

    @Override public PMultimap<K,V> minusAll(K key, Collection<? extends V> values) {
        PSet<V> newValues = get(key).minusAll(values);
        PMap<K,PSet<V>> newData = data.plus(key, newValues);
        return new HashTreePMultimap<>(newData);
    }

    @Override public PMultimap<K,V> minusAll(Collection<K> keys) {
        PMap<K,PSet<V>> newData = data.minusAll(keys);
        return new HashTreePMultimap<>(newData);
    }

    @Override public int size() {
        return data.size();
    }

    @Override public boolean isEmpty() {
        return data.isEmpty();
    }

    @Override public boolean containsKey(Object key) {
        return data.containsKey(key);
    }

    @Override public boolean containsEntry(Object key, Object value) {
        return data.containsKey(key) && data.get(key).contains(value);
    }

    @Override public PSet<V> get(K key) {
        return data.containsKey(key) ? data.get(key) : HashTreePSet.<V> empty();
    }

    @Override public Set<K> keySet() {
        return data.keySet();
    }

    @Override public PMap<K,PSet<V>> asPMap() {
        return data;
    }

    @Override public boolean containsValue(Object value) {
        throw new IllegalAccessError();
    }

    @Override public boolean put(K key, V value) {
        throw new IllegalAccessError();
    }

    @Override public boolean remove(Object key, Object value) {
        throw new IllegalAccessError();
    }

    @Override public boolean putAll(K key, Iterable<? extends V> values) {
        throw new IllegalAccessError();
    }

    @Override public boolean putAll(Multimap<? extends K,? extends V> multimap) {
        throw new IllegalAccessError();
    }

    @Override public Collection<V> replaceValues(K key, Iterable<? extends V> values) {
        throw new IllegalAccessError();
    }

    @Override public Collection<V> removeAll(Object key) {
        throw new IllegalAccessError();
    }

    @Override public void clear() {
        throw new IllegalAccessError();
    }

    @Override public Multiset<K> keys() {
        throw new IllegalAccessError();
    }

    @Override public Collection<V> values() {
        throw new IllegalAccessError();
    }

    @Override public Collection<Entry<K,V>> entries() {
        throw new IllegalAccessError();
    }

    @Override public Map<K,Collection<V>> asMap() {
        throw new IllegalAccessError();
    }

    @Override public String toString() {
        return data.toString();
    }

}