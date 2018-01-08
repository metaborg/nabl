package org.metaborg.meta.nabl2.util.collections;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.SetMultimap;

public class HashRelation3<K, L, V> implements IRelation3.Mutable<K, L, V>, Serializable {

    private static final long serialVersionUID = 42L;

    private final Map<K, SetMultimap<L, V>> fwd;
    private final Map<V, SetMultimap<L, K>> bwd;

    private HashRelation3(Map<K, SetMultimap<L, V>> fwd, Map<V, SetMultimap<L, K>> bwd) {
        this.fwd = fwd;
        this.bwd = bwd;
    }

    @Override public IRelation3.Mutable<V, L, K> inverse() {
        return new HashRelation3<>(bwd, fwd);
    }

    @Override public boolean contains(K key) {
        return fwd.containsKey(key) && !fwd.get(key).isEmpty();
    }

    @Override public boolean contains(K key, L label) {
        return fwd.containsKey(key) && fwd.get(key).containsKey(label) && !fwd.get(key).isEmpty();
    }

    @Override public boolean contains(K key, L label, V value) {
        return fwd.containsKey(key) && fwd.get(key).containsEntry(label, value);
    }

    @Override public boolean put(K key, L label, V value) {
        if(fwd.computeIfAbsent(key, k -> HashMultimap.create()).put(label, value)) {
            bwd.computeIfAbsent(value, v -> HashMultimap.create()).put(label, key);
            return true;
        }
        return false;
    }

    @Override public boolean remove(K key) {
        if(fwd.containsKey(key)) {
            Set<Entry<L, V>> entries = fwd.remove(key).entries();
            if(!entries.isEmpty()) {
                for(Map.Entry<L, V> entry : entries) {
                    bwd.get(entry.getValue()).remove(entry.getKey(), key);
                }
                return true;
            }
        }
        return false;
    }

    @Override public boolean remove(K key, L label) {
        if(fwd.containsKey(key)) {
            Set<V> values = fwd.get(key).removeAll(label);
            if(!values.isEmpty()) {
                for(V value : values) {
                    bwd.get(value).remove(label, key);
                }
                return true;
            }
        }
        return false;
    }

    @Override public boolean remove(K key, L label, V value) {
        if(fwd.containsKey(key) && fwd.get(key).remove(label, value)) {
            bwd.get(value).remove(label, key);
            return true;
        }
        return false;
    }

    @Override public ISet<K> keySet() {
        return WrappedSet.of(fwd.keySet());
    }

    @Override public ISet<V> valueSet() {
        return WrappedSet.of(bwd.keySet());
    }

    @Override public ISet<V> get(K key, L label) {
        return fwd.containsKey(key) ? WrappedSet.of(fwd.get(key).get(label)) : HashSet.create();
    }

    @Override public ISet<Map.Entry<L, V>> get(K key) {
        return fwd.containsKey(key) ? WrappedSet.of(fwd.get(key).entries()) : HashSet.create();
    }

    public static <K, L, V> HashRelation3<K, L, V> create() {
        return new HashRelation3<>(new HashMap<>(), new HashMap<>());
    }

    @Override public String toString() {
        return fwd.toString();
    }

    @Override public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + bwd.hashCode();
        result = prime * result + fwd.hashCode();
        return result;
    }

    @Override public boolean equals(Object obj) {
        if(this == obj)
            return true;
        if(obj == null)
            return false;
        if(getClass() != obj.getClass())
            return false;
        @SuppressWarnings("unchecked") final HashRelation3<K, L, V> other = (HashRelation3<K, L, V>) obj;
        if(!bwd.equals(other.bwd))
            return false;
        if(!fwd.equals(other.fwd))
            return false;
        return true;
    }
    
}