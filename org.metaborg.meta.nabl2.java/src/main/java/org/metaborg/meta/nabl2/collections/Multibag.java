package org.metaborg.meta.nabl2.collections;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

public class Multibag<K, V> {

    private final Map<K,List<V>> data;

    private Multibag() {
        this.data = Maps.newHashMap();
    }

    public Set<K> keySet() {
        return data.keySet();
    }

    public boolean isEmpty() {
        return data.isEmpty();
    }

    public Collection<V> get(K key) {
        return keyData(key);
    }

    public Collection<V> values() {
        List<V> result = Lists.newArrayList();
        for (List<V> vs : data.values()) {
            result.addAll(vs);
        }
        return result;
    }

    public void put(K key, V value) {
        keyData(key).add(value);
    }

    public void putAll(Multibag<K,V> other) {
        for (K key : other.keySet()) {
            keyData(key).addAll(Lists.newArrayList(other.get(key)));
        }
    }

    private List<V> keyData(K key) {
        return data.computeIfAbsent(key, k -> Lists.newArrayList());
    }

    public static <K, V> Multibag<K,V> create() {
        return new Multibag<>();
    }

}