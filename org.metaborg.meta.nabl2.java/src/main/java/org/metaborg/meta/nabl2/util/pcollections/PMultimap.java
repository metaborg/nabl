package org.metaborg.meta.nabl2.util.pcollections;

import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.pcollections.PMap;
import org.pcollections.PSet;

import com.google.common.collect.Multimap;
import com.google.common.collect.Multiset;

public interface PMultimap<K, V> extends Multimap<K,V> {

    PMultimap<K,V> plus(K key, V value);

    PMultimap<K,V> plusAll(K key, Collection<? extends V> values);

    PMultimap<K,V> plusAll(Multimap<? extends K,? extends V> multimap);

    PMultimap<K,V> minus(K key);

    PMultimap<K,V> minus(K key, V value);

    PMultimap<K,V> minusAll(K key, Collection<? extends V> values);

    PMultimap<K,V> minusAll(Collection<K> keys);

    @Override int size();

    @Override boolean isEmpty();

    @Override boolean containsKey(Object key);

    @Deprecated boolean containsValue(Object value);

    @Override boolean containsEntry(Object key, Object value);

    @Deprecated boolean put(K key, V value);

    @Deprecated boolean remove(Object key, Object value);

    @Deprecated boolean putAll(K key, Iterable<? extends V> values);

    @Deprecated boolean putAll(Multimap<? extends K,? extends V> multimap);

    @Deprecated Collection<V> replaceValues(K key, Iterable<? extends V> values);

    @Deprecated Collection<V> removeAll(Object key);

    @Deprecated void clear();

    @Override PSet<V> get(K key);

    @Override Set<K> keySet();

    @Deprecated Multiset<K> keys();

    @Deprecated Collection<V> values();

    @Deprecated Collection<Entry<K,V>> entries();

    @Deprecated Map<K,Collection<V>> asMap();

    PMap<K,PSet<V>> asPMap();

}
